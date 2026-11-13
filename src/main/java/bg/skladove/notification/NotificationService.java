
package bg.skladove.notification;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final AppUserRepository userRepository;

    public NotificationService(
            NotificationRepository notificationRepository,
            AppUserRepository userRepository) {
        this.notificationRepository = notificationRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void createNotification(
            AppUser recipient,
            Notification.Type type,
            String message) {

        if (recipient == null || type == null
                || message == null || message.isBlank()) {
            throw new IllegalArgumentException(
                    "Невалидни данни за известие.");
        }

        Notification notification =
                new Notification(recipient, type, message);

        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Потребителят не е намерен."));

        return notificationRepository
                .findByRecipientOrderByCreatedAtDesc(user);
    }

    @Transactional
    public void markAsRead(Long notificationId, String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Потребителят не е намерен."));

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Известието не е намерено."));

        if (!notification.getRecipient().getId().equals(user.getId())) {
            throw new IllegalArgumentException(
                    "Нямате право да променяте това известие.");
        }

        notification.setRead(true);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String username) {

        AppUser user = userRepository.findByUsername(username)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Потребителят не е намерен."));

        return notificationRepository
                .countByRecipientAndReadFalse(user);
    }

}
