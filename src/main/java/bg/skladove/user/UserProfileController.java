
package bg.skladove.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Controller
public class UserProfileController {

    private static final Logger logger =
            LogManager.getLogger(UserProfileController.class);

    private final AppUserRepository users;

    public UserProfileController(AppUserRepository users) {
        this.users = users;
    }

    // Display the logged-in user's profile
    @GetMapping("/profile")
    public String profile(
            java.security.Principal principal,
            Model model) {

        AppUser user = users.findByUsername(principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        logger.info("Profile accessed by user '{}'.", user.getUsername());

        model.addAttribute("user", user);

        return "user/profile";
    }

    // Update personal profile information
    @PostMapping("/profile")
    @Transactional
    public String updateProfile(
            java.security.Principal principal,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes) {

        AppUser user = users.findByUsername(principal.getName())
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (fullName == null || fullName.isBlank()) {
            logger.warn("Profile update failed for '{}': empty full name.",
                    user.getUsername());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Името е задължително.");
            return "redirect:/profile";
        }

        if (email != null && !email.isBlank()
                && !email.equalsIgnoreCase(user.getEmail())
                && users.existsByEmail(email)) {

            logger.warn("Profile update failed for '{}': email already registered.",
                    user.getUsername());

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Този имейл вече е регистриран.");
            return "redirect:/profile";
        }

        user.setFullName(fullName.trim());
        user.setEmail(email == null || email.isBlank()
                ? null : email.trim());
        user.setPhone(phone == null || phone.isBlank()
                ? null : phone.trim());

        users.save(user);

        logger.info("Profile successfully updated for user '{}'.",
                user.getUsername());

        redirectAttributes.addFlashAttribute(
                "successMessage", "Профилът е обновен успешно.");

        return "redirect:/profile";
    }

    // Admin changes an agent's commission percentage
    @PostMapping("/admin/users/{id}/fee")
    @Transactional
    public String updateAgentFee(
            @PathVariable Long id,
            @RequestParam BigDecimal fee,
            RedirectAttributes redirectAttributes) {

        AppUser agent = users.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("User not found."));

        if (agent.getRole() != Role.AGENT) {

            logger.warn("Honorarium update rejected: user ID {} is not an agent.",
                    id);

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Хонорар може да се задава само на агент.");
            return "redirect:/admin";
        }

        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0
                || fee.compareTo(new BigDecimal("100")) > 0) {

            logger.warn("Invalid honorarium value submitted for agent ID {}.",
                    id);

            redirectAttributes.addFlashAttribute(
                    "errorMessage", "Хонорарът трябва да бъде между 0 и 100%.");
            return "redirect:/admin";
        }

        agent.setFee(fee);
        users.save(agent);

        logger.info("Honorarium updated for agent '{}': {}%.",
                agent.getUsername(), fee);

        redirectAttributes.addFlashAttribute(
                "successMessage", "Хонорарът е обновен успешно.");

        return "redirect:/admin";
    }
}
