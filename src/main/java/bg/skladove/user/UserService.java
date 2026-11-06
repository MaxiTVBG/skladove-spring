
package bg.skladove.user;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final Logger logger =
            LogManager.getLogger(UserService.class);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository users,
                       PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public AppUser createUser(
            String username,
            String rawPassword,
            String fullName,
            String email,
            String phone,
            Role role) {

        if (username == null || username.isBlank()) {
            logger.warn("User creation failed: empty username.");
            throw new IllegalArgumentException(
                    "Username is required.");
        }

        if (rawPassword == null || rawPassword.length() < 6) {
            logger.warn("User creation failed for username '{}': invalid password length.",
                    username);
            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters.");
        }

        if (fullName == null || fullName.isBlank()) {
            logger.warn("User creation failed for username '{}': empty full name.",
                    username);
            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        if (users.existsByUsername(username)) {
            logger.warn("User creation failed: username '{}' already exists.",
                    username);
            throw new IllegalArgumentException(
                    "This username already exists.");
        }

        if (email != null && !email.isBlank()
                && users.existsByEmail(email)) {

            logger.warn("User creation failed: email already registered for username '{}'.",
                    username);
            throw new IllegalArgumentException(
                    "This email is already registered.");
        }

        AppUser user = new AppUser(
                username,
                passwordEncoder.encode(rawPassword),
                fullName,
                role
        );

        user.setEmail(email);
        user.setPhone(phone);

        AppUser savedUser = users.save(user);

        logger.info("New user created: username='{}', role='{}'.",
                savedUser.getUsername(), savedUser.getRole());

        return savedUser;
    }
}
