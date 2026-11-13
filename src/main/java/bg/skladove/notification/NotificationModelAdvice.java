
package bg.skladove.notification;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class NotificationModelAdvice {

    private final NotificationService notificationService;

    public NotificationModelAdvice(
            NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @ModelAttribute("unreadNotificationCount")
    public long unreadNotificationCount(
            Authentication authentication) {

        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            return 0;
        }

        return notificationService.getUnreadCount(
                authentication.getName());
    }
}
