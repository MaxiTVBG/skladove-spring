package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за справката „Складове на собственик“ (задача №14).
 * Най-важното е пресмятането на наетите дни, защото от него зависи заетостта в %.
 */
@ExtendWith(MockitoExtension.class)
class OwnerWarehouseReportServiceTest {

    private static final LocalDate NOV_1 = LocalDate.of(2026, 11, 1);
    private static final LocalDate NOV_30 = LocalDate.of(2026, 11, 30);

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private RentalContractRepository contracts;

    @Mock
    private AppUserRepository users;

    @InjectMocks
    private OwnerWarehouseReportService service;

    private AppUser owner;

    @BeforeEach
    void setUp() {
        owner = new AppUser("mario", "hash", "Марио", Role.OWNER);
        ReflectionTestUtils.setField(owner, "id", 1L);
    }

    private static Warehouse warehouse(Long id, String name) {
        Warehouse w = new Warehouse();
        ReflectionTestUtils.setField(w, "id", id);
        w.setName(name);
        return w;
    }

    /** Конструкторът на RentalContract е protected; BeanUtils го извиква през reflection. */
    private static RentalContract contract(Warehouse w, LocalDate start, LocalDate end, String price) {
        RentalContract c = BeanUtils.instantiateClass(RentalContract.class);
        c.setWarehouse(w);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setPrice(new BigDecimal(price));
        c.setStatus(RentalStatus.ACTIVE);
        return c;
    }

    // ---------------- Пресмятане на наетите дни ----------------

    @Test
    void contractInsidePeriodCountsAllItsDays() {
        Warehouse w = warehouse(10L, "Север");
        // 10–19 ноември = 10 дни (двете дати се броят)
        long days = OwnerWarehouseReportService.rentedDays(
                List.of(contract(w, LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 19), "100")),
                NOV_1, NOV_30);
        assertThat(days).isEqualTo(10);
    }

    @Test
    void contractIsCutToThePeriod() {
        Warehouse w = warehouse(10L, "Север");
        // договор от октомври до 5 ноември: в ноември влизат само 1–5 = 5 дни
        long days = OwnerWarehouseReportService.rentedDays(
                List.of(contract(w, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 11, 5), "100")),
                NOV_1, NOV_30);
        assertThat(days).isEqualTo(5);
    }

    @Test
    void overlappingDaysAreCountedOnce() {
        Warehouse w = warehouse(10L, "Север");
        // 1–10 и 5–15 се застъпват: заедно са 1–15 = 15 дни, не 21
        long days = OwnerWarehouseReportService.rentedDays(List.of(
                        contract(w, LocalDate.of(2026, 11, 5), LocalDate.of(2026, 11, 15), "100"),
                        contract(w, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 10), "100")),
                NOV_1, NOV_30);
        assertThat(days).isEqualTo(15);
    }

    @Test
    void separateContractsAreAdded() {
        Warehouse w = warehouse(10L, "Север");
        // 1–5 и 21–30: 5 + 10 = 15 дни
        long days = OwnerWarehouseReportService.rentedDays(List.of(
                        contract(w, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5), "100"),
                        contract(w, LocalDate.of(2026, 11, 21), LocalDate.of(2026, 11, 30), "100")),
                NOV_1, NOV_30);
        assertThat(days).isEqualTo(15);
    }

    @Test
    void noContractsMeansZeroDays() {
        assertThat(OwnerWarehouseReportService.rentedDays(List.of(), NOV_1, NOV_30)).isZero();
    }

    // ---------------- Цялата справка ----------------

    @Test
    void reportIncludesAllWarehousesWithOccupancyAndTotal() {
        Warehouse north = warehouse(10L, "Север");
        Warehouse south = warehouse(20L, "Юг"); // без договори в периода
        when(users.findByUsername("mario")).thenReturn(Optional.of(owner));
        when(warehouses.findByOwner(owner)).thenReturn(List.of(south, north));
        when(contracts.findOwnerContractsInPeriod(1L, OwnerWarehouseReportService.COUNTED_STATUSES, NOV_1, NOV_30))
                .thenReturn(List.of(
                        contract(north, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 15), "600.00"),
                        contract(north, LocalDate.of(2026, 11, 16), LocalDate.of(2026, 11, 30), "650.50")));

        List<OwnerWarehouseReportRow> rows = service.build("mario", NOV_1, NOV_30);

        assertThat(rows).hasSize(2);
        OwnerWarehouseReportRow northRow = rows.get(0); // подредени по име: „Север“ преди „Юг“
        assertThat(northRow.getWarehouse()).isSameAs(north);
        assertThat(northRow.getRentedDays()).isEqualTo(30);
        assertThat(northRow.getPeriodDays()).isEqualTo(30);
        assertThat(northRow.getOccupancyPercent()).isEqualTo(100);
        assertThat(northRow.getContractsTotal()).isEqualByComparingTo("1250.50");

        OwnerWarehouseReportRow southRow = rows.get(1);
        assertThat(southRow.getContracts()).isEmpty();
        assertThat(southRow.getOccupancyPercent()).isZero();
    }

    @Test
    void agentCannotSeeOwnerReport() {
        AppUser agent = new AppUser("petar", "hash", "Петър", Role.AGENT);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.build("petar", NOV_1, NOV_30))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> service.build("mario", NOV_30, NOV_1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Крайната дата трябва да е след началната.");
    }
}
