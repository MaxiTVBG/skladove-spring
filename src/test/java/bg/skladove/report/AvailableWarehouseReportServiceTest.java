package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import bg.skladove.warehouse.WarehouseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
 * Unit тестове за справката „Налични складове“ (задача №13).
 * WarehouseAvailabilityService е истинският (с mock на repository-то),
 * така че се проверява и реалната логика за застъпване на периоди.
 */
@ExtendWith(MockitoExtension.class)
class AvailableWarehouseReportServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 12, 1);
    private static final LocalDate TO = LocalDate.of(2026, 12, 31);

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private RentalContractRepository contracts;

    @Mock
    private AppUserRepository users;

    private AvailableWarehouseReportService service;

    private AppUser agent;

    @BeforeEach
    void setUp() {
        WarehouseAvailabilityService availability = new WarehouseAvailabilityService(contracts);
        service = new AvailableWarehouseReportService(warehouses, contracts, users, availability);
        agent = new AppUser("petar", "hash", "Петър", Role.AGENT);
    }

    private static Warehouse warehouse(Long id, String name, String category, WarehouseStatus status) {
        Warehouse w = new Warehouse();
        ReflectionTestUtils.setField(w, "id", id);
        w.setName(name);
        w.setAddress("Варна, ул. " + name);
        w.setCategory(category);
        w.setStatus(status);
        w.setMonthlyPrice(new BigDecimal("1000"));
        return w;
    }

    private static RentalContract active(Warehouse w, LocalDate start, LocalDate end) {
        RentalContract c = BeanUtils.instantiateClass(RentalContract.class); // конструкторът е protected
        c.setWarehouse(w);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setStatus(RentalStatus.ACTIVE);
        return c;
    }

    @Test
    void warehouseWithOverlappingContractIsNotAvailable() {
        Warehouse busy = warehouse(1L, "Зает", "A", WarehouseStatus.RENTED);
        Warehouse free = warehouse(2L, "Свободен", "A", WarehouseStatus.AVAILABLE);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(busy, free));
        // договорът 20.11 – 10.12 се застъпва с декември
        when(contracts.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(List.of(active(busy, LocalDate.of(2026, 11, 20), LocalDate.of(2026, 12, 10))));
        when(contracts.findByWarehouseIdAndStatus(2L, RentalStatus.ACTIVE)).thenReturn(List.of());

        List<AvailableWarehouse> rows = service.findAvailable("petar", FROM, TO, null, null);

        assertThat(rows).extracting(AvailableWarehouse::getWarehouse).containsExactly(free);
    }

    @Test
    void freeDatesAroundThePeriodAreCalculated() {
        Warehouse w = warehouse(1L, "Север", "A", WarehouseStatus.RENTED);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(w));
        when(contracts.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(List.of(
                active(w, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 25)),   // свършва преди периода
                active(w, LocalDate.of(2027, 1, 10), LocalDate.of(2027, 2, 10))));  // започва след него

        AvailableWarehouse row = service.findAvailable("petar", FROM, TO, null, null).get(0);

        assertThat(row.getFreeSince()).isEqualTo(LocalDate.of(2026, 11, 26));
        assertThat(row.getFreeUntil()).isEqualTo(LocalDate.of(2027, 1, 9));
    }

    @Test
    void warehouseUnderMaintenanceIsNotAvailable() {
        Warehouse repair = warehouse(1L, "Ремонт", "A", WarehouseStatus.MAINTENANCE);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(repair));

        assertThat(service.findAvailable("petar", FROM, TO, null, null)).isEmpty();
    }

    @Test
    void categoryAndSearchFilterTheResult() {
        Warehouse a = warehouse(1L, "Морска", "A", WarehouseStatus.AVAILABLE);
        Warehouse b = warehouse(2L, "Планинска", "B", WarehouseStatus.AVAILABLE);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(a, b));
        when(contracts.findByWarehouseIdAndStatus(2L, RentalStatus.ACTIVE)).thenReturn(List.of());

        List<AvailableWarehouse> rows = service.findAvailable("petar", FROM, TO, "B", "планин");

        assertThat(rows).extracting(AvailableWarehouse::getWarehouse).containsExactly(b);
    }

    @Test
    void adminCannotSeeTheReport() {
        AppUser admin = new AppUser("admin", "hash", "Администратор", Role.ADMIN);
        when(users.findByUsername("admin")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.findAvailable("admin", FROM, TO, null, null))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void endBeforeStartIsRejected() {
        assertThatThrownBy(() -> service.findAvailable("petar", TO, FROM, null, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
