package bg.skladove.config;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/** При първо стартиране създава администратор, за да може да се влезе в системата. */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LogManager.getLogger(DataInitializer.class);

    private final AppUserRepository users;
    private final PasswordEncoder encoder;
    private final String adminPassword;

    public DataInitializer(AppUserRepository users, PasswordEncoder encoder,
                           @Value("${app.admin.password}") String adminPassword) {
        this.users = users;
        this.encoder = encoder;
        this.adminPassword = adminPassword;
    }

    @Override
    public void run(String... args) {
        if (users.findByUsername("admin").isEmpty()) {
            users.save(new AppUser("admin", encoder.encode(adminPassword), "Администратор", Role.ADMIN));
            log.info("Създаден е администратор 'admin'");
        }
    }
}
