
package bg.skladove.notification;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository
        extends JpaRepository<Notification, Long> {

    List<Notification> findByRecipientOrderByCreatedAtDesc(
            AppUser recipient);

    boolean existsByRecipientAndTypeAndMessage(
            AppUser recipient,
            Notification.Type type,
            String message);

    long countByRecipientAndReadFalse(AppUser recipient);
}
