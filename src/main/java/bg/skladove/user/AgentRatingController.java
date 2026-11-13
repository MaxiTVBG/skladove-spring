
package bg.skladove.user;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/owner")
public class AgentRatingController {

    private final AgentRatingService ratingService;

    public AgentRatingController(AgentRatingService ratingService) {
        this.ratingService = ratingService;
    }

    // Display completed rentals that can be rated
    @GetMapping("/ratings")
    public String ratings(Principal principal, Model model) {

        model.addAttribute(
                "contracts",
                ratingService.findRateableContracts(principal.getName())
        );

        return "owner/warehouses/ratings";
    }

    // Submit an agent rating
    @PostMapping("/ratings")
    public String submitRating(
            @RequestParam Long contractId,
            @RequestParam Integer score,
            @RequestParam(required = false) String comment,
            Principal principal,
            RedirectAttributes redirectAttributes) {

        try {
            ratingService.submitRating(
                    contractId,
                    principal.getName(),
                    score,
                    comment
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Оценката е изпратена успешно."
            );

        } catch (IllegalArgumentException e) {

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/owner/ratings";
    }
}
