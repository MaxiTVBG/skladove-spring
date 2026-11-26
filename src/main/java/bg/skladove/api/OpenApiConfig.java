package bg.skladove.api;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Описание на API-то за Swagger UI (задача №16).
 *
 * Документацията е на /swagger-ui.html, а JSON спецификацията (OpenAPI 3) — на /v3/api-docs.
 * В нея влизат само REST адресите (/api/**, /owner/api/**, /agent/api/**, виж application.yml),
 * а не HTML страниците.
 *
 * Удостоверяването е със сесия: потребителят влиза през /login и браузърът праща бисквитката
 * JSESSIONID и към Swagger UI, така че „Try it out“ работи с правата на влезлия потребител.
 */
@Configuration
public class OpenApiConfig {

    static final String SESSION_SCHEME = "session";

    @Bean
    public OpenAPI skladoveOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Складове API")
                        .version("1.0")
                        .description("""
                                REST API на системата за отдаване на складове под наем.

                                **Как се използва:** влезте в приложението през `/login` с потребител с нужната роля \
                                (собственик или складов агент) и отворете тази страница в същия браузър. \
                                Заявките от „Try it out“ се изпращат с вашата сесия.

                                * **Собственик** – адреси под `/owner/api` (роля OWNER)
                                * **Складов агент** – адреси под `/agent/api` (роля AGENT)
                                * **Справки** – `/api/reports` (собственици и агенти)

                                Без вход адресите връщат 401, а с друга роля – 403.
                                """))
                .components(new Components().addSecuritySchemes(SESSION_SCHEME, new SecurityScheme()
                        .type(SecurityScheme.Type.APIKEY)
                        .in(SecurityScheme.In.COOKIE)
                        .name("JSESSIONID")
                        .description("Бисквитката на сесията след вход през /login")))
                .addSecurityItem(new SecurityRequirement().addList(SESSION_SCHEME));
    }

    /**
     * Всички адреси изискват вход и определена роля, затова 401 и 403 се добавят
     * към всеки endpoint тук, вместо да се пишат при всеки метод поотделно.
     */
    @Bean
    public OpenApiCustomizer securityResponses() {
        return openApi -> openApi.getPaths().values().forEach(path ->
                path.readOperations().forEach(operation -> {
                    ApiResponses responses = operation.getResponses();
                    if (!responses.containsKey("401")) {
                        responses.addApiResponse("401", new ApiResponse()
                                .description("Не сте влезли в системата (влезте през /login)"));
                    }
                    if (!responses.containsKey("403")) {
                        responses.addApiResponse("403", new ApiResponse()
                                .description("Нямате права – адресът е за друга роля"));
                    }
                }));
    }
}
