package bg.skladove.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

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
            throw new IllegalArgumentException(
                    "Username is required.");
        }

        if (rawPassword == null || rawPassword.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters.");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException(
                    "Full name is required.");
        }

        if (users.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "This username already exists.");
        }

        if (email != null && !email.isBlank()
                && users.existsByEmail(email)) {
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

        return users.save(user);
    }
}