package bg.skladove;

import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SkladoveApplicationTests {

    @Autowired
    private AppUserRepository users;

    @Test
    void contextLoads() {
    }

    @Test
    void adminIsCreatedOnStartup() {
        assertThat(users.findByUsername("admin"))
                .isPresent()
                .get()
                .extracting(u -> u.getRole())
                .isEqualTo(Role.ADMIN);
    }
}
