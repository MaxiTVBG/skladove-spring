package bg.skladove.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Достъп по роли:
 *  /admin/**  - само администратор
 *  /owner/**  - собственик
 *  /agent/**  - складов агент
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/css/**", "/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/owner/**").hasRole("OWNER")
                        .requestMatchers("/agent/**").hasRole("AGENT")
                        .anyRequest().authenticated())
                .formLogin(form -> form.loginPage("/login").defaultSuccessUrl("/", true).permitAll())
                .logout(logout -> logout.logoutSuccessUrl("/login?logout"));
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
