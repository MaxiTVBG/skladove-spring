package bg.skladove;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Входна точка на системата за складове.
 * EnableScheduling е нужен за известията за изтичащи договори.
 */
@SpringBootApplication
@EnableScheduling
public class SkladoveApplication {

    public static void main(String[] args) {
        SpringApplication.run(SkladoveApplication.class, args);
    }
}
