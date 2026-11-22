package bg.skladove.rental;

import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDate;

/**
 * Страниците на складовия агент: складовете, които обслужва,
 * формулярът за наем и списъкът с неговите заявки и договори.
 *
 * Всичко под /agent/** е достъпно само за роля AGENT (виж SecurityConfig).
 */
@Controller
@RequestMapping("/agent")
public class AgentRentalController {

    private final RentalService rentalService;
    private final RentalQueryService queries;

    public AgentRentalController(RentalService rentalService, RentalQueryService queries) {
        this.rentalService = rentalService;
        this.queries = queries;
    }

    /** /agent води към списъка (линкът „Наеми“ от началната страница). */
    @GetMapping
    public String home() {
        return "redirect:/agent/rentals";
    }

    /** Складовете, които агентът обслужва, и неговите заявки/договори. */
    @GetMapping("/rentals")
    public String list(Model model, Principal principal) {
        model.addAttribute("warehouses", queries.warehousesForAgent(principal.getName()));
        model.addAttribute("contracts", queries.contractsForAgent(principal.getName()));
        return "agent/rentals/list";
    }

    /**
     * Празен формуляр за наем на избран склад. Цената се попълва с месечната цена на склада.
     * Ако се идва от препоръката на склад, периодът (from, to) също се попълва.
     */
    @GetMapping("/rentals/new")
    public String form(@RequestParam Long warehouseId,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                       Model model, Principal principal) {
        Warehouse warehouse = queries.warehouseForAgent(warehouseId, principal.getName());

        RentalRequestForm form = new RentalRequestForm();
        form.setWarehouseId(warehouseId);
        form.setStartDate(from != null ? from : LocalDate.now());
        form.setEndDate(to);
        form.setPrice(warehouse.getMonthlyPrice());

        model.addAttribute("rentalForm", form);
        model.addAttribute("warehouse", warehouse);
        return "agent/rentals/form";
    }

    /** Изпраща заявката към собственика за одобрение. */
    @PostMapping("/rentals")
    public String create(@Valid @ModelAttribute("rentalForm") RentalRequestForm form,
                         BindingResult errors,
                         Model model,
                         Principal principal,
                         RedirectAttributes redirect) {
        Warehouse warehouse = queries.warehouseForAgent(form.getWarehouseId(), principal.getName());
        model.addAttribute("warehouse", warehouse);

        if (errors.hasErrors()) {
            return "agent/rentals/form";
        }

        Long agentId = queries.getUser(principal.getName(), Role.AGENT).getId();
        try {
            rentalService.createRequest(
                    form.getWarehouseId(), agentId,
                    form.getTenantName().trim(), form.getTenantPhone(), form.getTenantEmail(),
                    form.getStartDate(), form.getEndDate(), form.getPrice());
        } catch (IllegalArgumentException | IllegalStateException e) {
            // Бизнес правило от RentalService, напр. „Складът не е свободен за избрания период.“
            model.addAttribute("errorMessage", e.getMessage());
            return "agent/rentals/form";
        }

        redirect.addFlashAttribute("successMessage",
                "Заявката за „" + warehouse.getName() + "“ е изпратена на собственика за одобрение.");
        return "redirect:/agent/rentals";
    }

    /** Несъществуващ склад: връща към списъка със съобщение. */
    @ExceptionHandler(IllegalArgumentException.class)
    public String handleNotFound(IllegalArgumentException e, RedirectAttributes redirect) {
        redirect.addFlashAttribute("errorMessage", e.getMessage());
        return "redirect:/agent/rentals";
    }
}
