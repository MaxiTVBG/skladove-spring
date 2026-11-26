package bg.skladove;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalService;
import bg.skladove.rental.RentalStatus;
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
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Интеграционни и функционални тестове за уеб процесите на собственик и складов агент
 * (задачи №10, №11, №12, №13, №14, №15).
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WarehouseRentalIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private WarehouseRepository warehouses;

    @Autowired
    private RentalContractRepository contracts;

    @Autowired
    private RentalService rentalService;

    private AppUser owner;
    private AppUser otherOwner;
    private AppUser agent;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        owner = users.findByUsername("int-owner")
                .orElseGet(() -> users.save(new AppUser("int-owner", "hash", "Иван Собственик", Role.OWNER)));
        otherOwner = users.findByUsername("int-other-owner")
                .orElseGet(() -> users.save(new AppUser("int-other-owner", "hash", "Мария Собственик", Role.OWNER)));
        agent = users.findByUsername("int-agent")
                .orElseGet(() -> users.save(new AppUser("int-agent", "hash", "Петър Агент", Role.AGENT)));

        warehouse = new Warehouse();
        warehouse.setName("Интеграционен склад");
        warehouse.setAddress("Варна, Западна промишлена зона");
        warehouse.setArea(300.0);
        warehouse.setHeight(7.0);
        warehouse.setCategory("A");
        warehouse.setClimateConditions("Сух");
        warehouse.setGoodsType("Палети");
        warehouse.setMonthlyPrice(new BigDecimal("2100.00"));
        warehouse.setOwner(owner);
        warehouse.getAgents().add(agent);
        warehouse = warehouses.save(warehouse);
    }

    @Test
    @WithMockUser(username = "int-owner", roles = "OWNER")
    void ownerCreatesAndUpdatesWarehouseWithAssignedAgent() throws Exception {
        mvc.perform(post("/owner/warehouses")
                        .with(csrf())
                        .param("name", "Нов склад Изток")
                        .param("address", "Варна, бул. Владислав 10")
                        .param("area", "150")
                        .param("height", "5.5")
                        .param("category", "B")
                        .param("climateConditions", "Отопляем")
                        .param("goodsType", "Техника")
                        .param("monthlyPrice", "1350")
                        .param("agentIds", agent.getId().toString()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/warehouses"))
                .andExpect(flash().attributeExists("successMessage"));

        List<Warehouse> owned = warehouses.findByOwner(owner);
        Warehouse created = owned.stream()
                .filter(w -> "Нов склад Изток".equals(w.getName()))
                .findFirst()
                .orElseThrow();
        assertThat(created.getAgents()).extracting(AppUser::getUsername).containsExactly("int-agent");

        mvc.perform(post("/owner/warehouses/" + created.getId())
                        .with(csrf())
                        .param("name", "Нов склад Изток (обновен)")
                        .param("address", "Варна, бул. Владислав 10")
                        .param("area", "160")
                        .param("height", "5.5")
                        .param("category", "A")
                        .param("climateConditions", "Отопляем")
                        .param("goodsType", "Техника")
                        .param("monthlyPrice", "1500"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/warehouses"));

        assertThat(warehouses.findById(created.getId()).orElseThrow().getName())
                .isEqualTo("Нов склад Изток (обновен)");
    }

    @Test
    @WithMockUser(username = "int-other-owner", roles = "OWNER")
    void otherOwnerCannotEditWarehouse() throws Exception {
        mvc.perform(get("/owner/warehouses/" + warehouse.getId() + "/edit"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "int-agent", roles = "AGENT")
    void agentSubmitsRentalRequest() throws Exception {
        mvc.perform(post("/agent/rentals")
                        .with(csrf())
                        .param("warehouseId", warehouse.getId().toString())
                        .param("tenantName", "Логистик БГ ООД")
                        .param("tenantPhone", "0888999000")
                        .param("tenantEmail", "rent@logistik.bg")
                        .param("startDate", "2027-02-01")
                        .param("endDate", "2027-02-28")
                        .param("price", "2100.00"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/agent/rentals"))
                .andExpect(flash().attributeExists("successMessage"));

        List<RentalContract> agentContracts = contracts.findByAgent(agent);
        assertThat(agentContracts).hasSize(1);
        assertThat(agentContracts.get(0).getStatus()).isEqualTo(RentalStatus.PENDING_OWNER_APPROVAL);
        assertThat(agentContracts.get(0).getTenantName()).isEqualTo("Логистик БГ ООД");
    }

    @Test
    @WithMockUser(username = "int-owner", roles = "OWNER")
    void ownerApprovesAndRejectsRentalRequests() throws Exception {
        RentalContract pending1 = rentalService.createRequest(
                warehouse.getId(), agent.getId(), "Наемател 1", null, null,
                LocalDate.of(2027, 3, 1), LocalDate.of(2027, 3, 31), new BigDecimal("2100"));

        RentalContract pending2 = rentalService.createRequest(
                warehouse.getId(), agent.getId(), "Наемател 2", null, null,
                LocalDate.of(2027, 4, 1), LocalDate.of(2027, 4, 30), new BigDecimal("1900"));

        mvc.perform(post("/owner/rentals/" + pending1.getId() + "/approve").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/rentals"))
                .andExpect(flash().attributeExists("successMessage"));

        assertThat(contracts.findById(pending1.getId()).orElseThrow().getStatus())
                .isEqualTo(RentalStatus.ACTIVE);

        mvc.perform(post("/owner/rentals/" + pending2.getId() + "/reject")
                        .with(csrf())
                        .param("note", "Недостатъчен срок"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/owner/rentals"))
                .andExpect(flash().attributeExists("successMessage"));

        RentalContract rejected = contracts.findById(pending2.getId()).orElseThrow();
        assertThat(rejected.getStatus()).isEqualTo(RentalStatus.REJECTED);
        assertThat(rejected.getDecisionNote()).isEqualTo("Недостатъчен срок");
    }

    @Test
    @WithMockUser(username = "int-agent", roles = "AGENT")
    void roleAccessControlBlocksCrossRolePages() throws Exception {
        mvc.perform(get("/owner/warehouses")).andExpect(status().isForbidden());
        mvc.perform(get("/owner/reports/warehouses")).andExpect(status().isForbidden());
        mvc.perform(get("/owner/ai/price-model")).andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "int-owner", roles = "OWNER")
    void ownerReportsAndAiPriceModelPagesLoad() throws Exception {
        mvc.perform(get("/reports/available")
                        .param("from", "2027-05-01")
                        .param("to", "2027-05-31"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("rows"));

        mvc.perform(get("/owner/reports/warehouses")
                        .param("from", "2027-05-01")
                        .param("to", "2027-05-31"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("rows"));

        mvc.perform(get("/owner/ai/price-model"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("evaluations"));
    }

    @Test
    @WithMockUser(username = "int-agent", roles = "AGENT")
    void agentAiRecommendationPageLoads() throws Exception {
        mvc.perform(get("/agent/ai/recommend")
                        .param("area", "200")
                        .param("climate", "Сух"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("matches"));
    }
}
