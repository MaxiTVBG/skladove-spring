package bg.skladove.notification;

import bg.skladove.user.AppUser;
import jakarta.persistence.*;

import java.time.LocalDateTime;

/** Известие в профила на потребител. */
@Entity
public class Notification {

    public enum Type { NEW_RENTAL_REQUEST, WAREHOUSE_RENTED, CONTRACT_EXPIRING }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private AppUser recipient;

    @Enumerated(EnumType.STRING)
    private Type type;

    private String message;

    private boolean read = false;

    private LocalDateTime createdAt = LocalDateTime.now();

    protected Notification() {
    }

    public Notification(AppUser recipient, Type type, String message) {
        this.recipient = recipient;
        this.type = type;
        this.message = message;
    }

    public Long getId() { return id; }
    public AppUser getRecipient() { return recipient; }
    public Type getType() { return type; }
    public String getMessage() { return message; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
