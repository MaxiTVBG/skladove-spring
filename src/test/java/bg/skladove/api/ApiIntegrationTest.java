package bg.skladove.api;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционни тестове за REST API-то и Swagger документацията (задача №16).
 *
 * Стартира се цялото приложение с H2 база в паметта (src/test/resources/application.yml),
 * а заявките минават през Spring Security, както в реалната работа.
 */
@SpringBootTest
@AutoConfigureMockMvc
class ApiIntegrationTest {

    private static final String OWNER = "api-owner";
    private static final String AGENT = "api-agent";

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private WarehouseRepository warehouses;

    /** Собственик с един склад и агент, назначен към него. Създават се веднъж за всички тестове. */
    @BeforeEach
    void setUp() {
        AppUser owner = users.findByUsername(OWNER)
                .orElseGet(() -> users.save(new AppUser(OWNER, "hash", "API Собственик", Role.OWNER)));
        AppUser agent = users.findByUsername(AGENT)
                .orElseGet(() -> users.save(new AppUser(AGENT, "hash", "API Агент", Role.AGENT)));

        if (warehouses.findByOwner(owner).isEmpty()) {
            Warehouse w = new Warehouse();
            w.setName("API склад");
            w.setAddress("Варна, ул. Тестова 1");
            w.setArea(100.0);
            w.setHeight(6.0);
            w.setCategory("B");
            w.setClimateConditions("Сух");
            w.setGoodsType("Стоки");
            w.setMonthlyPrice(new BigDecimal("900"));
            w.setOwner(owner);
            w.getAgents().add(agent);
            warehouses.save(w);
        }
    }

    // ---------------- Swagger / OpenAPI ----------------

    @Test
    void apiDocsDescribeTheRestEndpointsOnly() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Складове API"))
                .andExpect(jsonPath("$.paths['/owner/api/warehouses']").exists())
                .andExpect(jsonPath("$.paths['/owner/api/reports/warehouses']").exists())
                .andExpect(jsonPath("$.paths['/owner/api/ai/price-recommendation']").exists())
                .andExpect(jsonPath("$.paths['/agent/api/ai/recommend']").exists())
                .andExpect(jsonPath("$.paths['/api/reports/available']").exists())
                // HTML страниците не са част от API документацията
                .andExpect(jsonPath("$.paths['/owner/warehouses']").doesNotExist());
    }

    @Test
    void swaggerUiIsAvailableWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
    }

    // ---------------- Сигурност ----------------

    @Test
    void anonymousUserGets401() throws Exception {
        mvc.perform(get("/owner/api/warehouses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = AGENT, roles = "AGENT")
    void agentCannotUseOwnerApi() throws Exception {
        mvc.perform(get("/owner/api/warehouses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void adminCannotSeeAvailableWarehousesReport() throws Exception {
        mvc.perform(get("/api/reports/available"))
                .andExpect(status().isForbidden());
    }

    // ---------------- Данни ----------------

    @Test
    @WithMockUser(username = OWNER, roles = "OWNER")
    void ownerSeesOwnWarehousesWithAgents() throws Exception {
        mvc.perform(get("/owner/api/warehouses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("API склад"))
                .andExpect(jsonPath("$[0].agents[0]").value(AGENT));
    }

    @Test
    @WithMockUser(username = OWNER, roles = "OWNER")
    void invalidPeriodGives400WithMessage() throws Exception {
        mvc.perform(get("/owner/api/reports/warehouses")
                        .param("from", "2026-12-31")
                        .param("to", "2026-12-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Крайната дата трябва да е след началната."));
    }

    @Test
    @WithMockUser(username = AGENT, roles = "AGENT")
    void agentSeesAvailableWarehouseInReport() throws Exception {
        mvc.perform(get("/api/reports/available")
                        .param("from", "2030-01-01")
                        .param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouse.name").value("API склад"));
    }

    @Test
    @WithMockUser(username = AGENT, roles = "AGENT")
    void agentGetsWarehouseRecommendation() throws Exception {
        mvc.perform(get("/agent/api/ai/recommend")
                        .param("area", "100")
                        .param("climate", "Сух")
                        .param("from", "2030-01-01")
                        .param("to", "2030-01-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].warehouse.name").value("API склад"))
                .andExpect(jsonPath("$[0].matchPercent").value(100))
                .andExpect(jsonPath("$[0].estimatedPrice").isNumber());
    }

    @Test
    @WithMockUser(username = OWNER, roles = "OWNER")
    void ownerGetsPriceRecommendation() throws Exception {
        mvc.perform(get("/owner/api/ai/price-recommendation")
                        .param("area", "120")
                        .param("category", "B")
                        .param("climate", "Хладилен"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendedPrice").isNumber())
                .andExpect(jsonPath("$.neighbors.length()").value(3));
    }
}
