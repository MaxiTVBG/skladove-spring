package bg.skladove.rental;

import bg.skladove.user.Role;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;

/**
 * Страницата на собственика за заявките за наем на неговите складове:
 * одобряване, отхвърляне и история на договорите.
 *
 * Адресът е под /owner/**, затова е достъпен само за роля OWNER.
 * Дали заявката е за склад на този собственик проверява RentalService.
 */
@Controller
@RequestMapping("/owner/rentals")
public class OwnerRentalController {

    private final RentalService rentalService;
    private final RentalQueryService queries;

    public OwnerRentalController(RentalService rentalService, RentalQueryService queries) {
        this.rentalService = rentalService;
        this.queries = queries;
    }

    /** Чакащите заявки отгоре, а под тях всички останали договори. */
    @GetMapping
    public String list(Model model, Principal principal) {
        List<RentalContract> all = queries.contractsForOwner(principal.getName());

        model.addAttribute("pending", all.stream()
                .filter(c -> c.getStatus() == RentalStatus.PENDING_OWNER_APPROVAL)
                .toList());
        model.addAttribute("history", all.stream()
                .filter(c -> c.getStatus() != RentalStatus.PENDING_OWNER_APPROVAL)
                .toList());
        return "owner/rentals/list";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        Long ownerId = queries.getUser(principal.getName(), Role.OWNER).getId();
        try {
            rentalService.approveRequest(id, ownerId);
            redirect.addFlashAttribute("successMessage", "Заявката е одобрена. Договорът е активен.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/owner/rentals";
    }

    @PostMapping("/{id}/reject")
    public String reject(@PathVariable Long id,
                         @RequestParam(required = false) String note,
                         Principal principal,
                         RedirectAttributes redirect) {
        Long ownerId = queries.getUser(principal.getName(), Role.OWNER).getId();
        String cleanNote = (note == null || note.isBlank()) ? null : note.trim();
        try {
            rentalService.rejectRequest(id, ownerId, cleanNote);
            redirect.addFlashAttribute("successMessage", "Заявката е отхвърлена.");
        } catch (IllegalArgumentException | IllegalStateException e) {
            redirect.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/owner/rentals";
    }
}
