
package bg.skladove.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger logger =
            LogManager.getLogger(AdminController.class);

    private final AppUserRepository users;
    private final UserService userService;

    public AdminController(AppUserRepository users,
                           UserService userService) {
        this.users = users;
        this.userService = userService;
    }

    @GetMapping
    public String dashboard(Model model) {

        logger.info("Admin dashboard accessed.");

        model.addAttribute("users", users.findAll());

        return "admin/dashboard";
    }

    @PostMapping("/users/owner")
    public String createOwner(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes) {

        return createUser(
                username, password, fullName,
                email, phone, Role.OWNER, redirectAttributes
        );
    }

    @PostMapping("/users/agent")
    public String createAgent(
            @RequestParam String username,
            @RequestParam String password,
            @RequestParam String fullName,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String phone,
            RedirectAttributes redirectAttributes) {

        return createUser(
                username, password, fullName,
                email, phone, Role.AGENT, redirectAttributes
        );
    }

    private String createUser(
            String username,
            String password,
            String fullName,
            String email,
            String phone,
            Role role,
            RedirectAttributes redirectAttributes) {

        try {
            userService.createUser(
                    username, password, fullName,
                    email, phone, role
            );

            redirectAttributes.addFlashAttribute(
                    "successMessage",
                    "Потребителят е създаден успешно."
            );

        } catch (IllegalArgumentException e) {

            logger.warn("Admin user creation failed for username '{}', role '{}': {}",
                    username, role, e.getMessage());

            redirectAttributes.addFlashAttribute(
                    "errorMessage",
                    e.getMessage()
            );
        }

        return "redirect:/admin";
    }
}
