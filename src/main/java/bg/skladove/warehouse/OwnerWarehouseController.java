package bg.skladove.warehouse;

import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Страниците на собственика за неговите складове.
 *
 * Всичко под /owner/** е достъпно само за роля OWNER (виж SecurityConfig).
 * Principal е влезлият потребител, който Spring Security подава автоматично.
 */
@Controller
@RequestMapping("/owner")
public class OwnerWarehouseController {

    /** Категориите, които се показват в падащото меню на формата. */
    static final List<String> CATEGORIES = List.of("A", "B", "C");

    private final WarehouseService warehouseService;

    public OwnerWarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    /** /owner води директно към списъка (линкът „Моите складове“ от началната страница). */
    @GetMapping
    public String home() {
        return "redirect:/owner/warehouses";
    }

    /** Списък с моите складове. */
    @GetMapping("/warehouses")
    public String list(Model model, Principal principal) {
        model.addAttribute("warehouses", warehouseService.findByOwner(principal.getName()));
        return "owner/warehouses/list";
    }

    /** Празна форма за нов склад. */
    @GetMapping("/warehouses/new")
    public String createForm(Model model) {
        model.addAttribute("warehouseForm", new WarehouseForm());
        return showForm(model, null);
    }

    /** Записва нов склад. При грешка във валидацията показва формата отново. */
    @PostMapping("/warehouses")
    public String create(@Valid @ModelAttribute("warehouseForm") WarehouseForm form,
                         BindingResult errors,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return showForm(model, null);
        }
        Warehouse saved = warehouseService.create(form, principal.getName());
        redirect.addFlashAttribute("successMessage", "Складът „" + saved.getName() + "“ е добавен.");
        return "redirect:/owner/warehouses";
    }

    /** Форма за редакция, попълнена с текущите данни на склада. */
    @GetMapping("/warehouses/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, Principal principal) {
        Warehouse warehouse = warehouseService.getOwnedWarehouse(id, principal.getName());
        model.addAttribute("warehouseForm", WarehouseForm.from(warehouse));
        return showForm(model, id);
    }

    /** Записва промените по склада. */
    @PostMapping("/warehouses/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("warehouseForm") WarehouseForm form,
                         BindingResult errors,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirect) {
        if (errors.hasErrors()) {
            return showForm(model, id);
        }
        Warehouse saved = warehouseService.update(id, form, principal.getName());
        redirect.addFlashAttribute("successMessage", "Складът „" + saved.getName() + "“ е обновен.");
        return "redirect:/owner/warehouses";
    }

    /** Несъществуващ склад или невалиден агент: връща към списъка със съобщение вместо страница с грешка. */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleNotFound(IllegalArgumentException e, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/owner/warehouses";
    }

    /** Една и съща страница служи и за добавяне (id = null), и за редакция. */
    private String showForm(Model model, Long warehouseId) {
        model.addAttribute("warehouseId", warehouseId);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("agents", warehouseService.findAllAgents());
        return "owner/warehouses/form";
    }
}
