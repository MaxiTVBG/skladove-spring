package bg.skladove.user;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

/**
 * Потребител: администратор, собственик или складов агент.
 * Хонорарът и рейтингът се ползват само за складови агенти.
 */
@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true)
    private String username;

    @NotBlank
    @Column(nullable = false)
    private String password;

    @NotBlank
    private String fullName;

    @Email
    private String email;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    /** Хонорар на складов агент (процент или сума, уточнява се в анализа). */
    private BigDecimal fee;

    /** Среден рейтинг на складов агент. */
    private Double rating;

    protected AppUser() {
    }

    public AppUser(String username, String password, String fullName, Role role) {
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public BigDecimal getFee() { return fee; }
    public void setFee(BigDecimal fee) { this.fee = fee; }
    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }
}
