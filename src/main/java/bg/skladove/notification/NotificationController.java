
package bg.skladove.notification;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService notificationService;

    public NotificationController(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public String showNotifications(
            Authentication authentication,
            Model model) {

        model.addAttribute(
                "notifications",
                notificationService.getNotifications(
                        authentication.getName()));

        return "notifications/list";
    }

    @PostMapping("/{id}/read")
    public String markAsRead(
            @PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {

        try {
            notificationService.markAsRead(
                    id, authentication.getName());

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Известието е маркирано като прочетено.");

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute(
                    "errorMessage", e.getMessage());
        }

        return "redirect:/notifications";
    }
}
