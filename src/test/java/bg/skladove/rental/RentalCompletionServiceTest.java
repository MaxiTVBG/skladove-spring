package bg.skladove.rental;

import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import bg.skladove.warehouse.WarehouseStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за автоматичното приключване на договори (задача №12).
 *
 * Mock-ват се само repository-тата (интерфейси). WarehouseAvailabilityService е истинският,
 * така че тестът проверява и реалното пресмятане на статуса на склада.
 * (Mock на конкретен клас не работи с Java 25 и версията на Mockito в проекта.)
 */
@ExtendWith(MockitoExtension.class)
class RentalCompletionServiceTest {

    // WarehouseAvailabilityService.synchronizeStatus ползва LocalDate.now(),
    // затова и тестът работи с реалната днешна дата.
    private static final LocalDate TODAY = LocalDate.now();

    @Mock
    private RentalContractRepository contracts;

    @Mock
    private WarehouseRepository warehouses;

    private RentalCompletionService service;

    @BeforeEach
    void setUp() {
        WarehouseAvailabilityService availability = new WarehouseAvailabilityService(contracts);
        service = new RentalCompletionService(contracts, warehouses, availability);
    }

    /** Warehouse няма setId (id-то идва от базата), затова го задаваме с reflection. */
    private static Warehouse warehouse(Long id, String name, WarehouseStatus status) {
        Warehouse w = new Warehouse();
        ReflectionTestUtils.setField(w, "id", id);
        w.setName(name);
        w.setStatus(status);
        return w;
    }

    private static RentalContract contract(Warehouse w, LocalDate start, LocalDate end) {
        RentalContract c = new RentalContract(); // конструкторът е protected, тестът е в същия пакет
        c.setWarehouse(w);
        c.setStartDate(start);
        c.setEndDate(end);
        c.setStatus(RentalStatus.ACTIVE);
        return c;
    }

    @Test
    void expiredActiveContractBecomesCompletedAndWarehouseIsFreed() {
        Warehouse north = warehouse(1L, "Склад Север", WarehouseStatus.RENTED);
        RentalContract expired = contract(north, TODAY.minusMonths(2), TODAY.minusDays(1));

        when(contracts.findByStatusAndEndDateBefore(RentalStatus.ACTIVE, TODAY)).thenReturn(List.of(expired));
        when(contracts.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of());
        // след приключването складът няма други активни договори
        when(contracts.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(List.of());

        int completed = service.completeExpiredRentals(TODAY);

        assertThat(completed).isEqualTo(1);
        assertThat(expired.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(north.getStatus()).isEqualTo(WarehouseStatus.AVAILABLE);
        verify(warehouses).save(north);
    }

    @Test
    void nothingExpiredMeansNothingChanges() {
        // Договор с край днес не се връща от findByStatusAndEndDateBefore(ACTIVE, днес):
        // последният ден от наема все още е ден на наема.
        when(contracts.findByStatusAndEndDateBefore(RentalStatus.ACTIVE, TODAY)).thenReturn(List.of());
        when(contracts.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of());

        int completed = service.completeExpiredRentals(TODAY);

        assertThat(completed).isZero();
        verify(warehouses, never()).save(any());
    }

    @Test
    void warehouseWithContractStartingTodayBecomesRented() {
        Warehouse south = warehouse(2L, "Склад Юг", WarehouseStatus.AVAILABLE);
        RentalContract startsToday = contract(south, TODAY, TODAY.plusMonths(1));

        when(contracts.findByStatusAndEndDateBefore(RentalStatus.ACTIVE, TODAY)).thenReturn(List.of());
        when(contracts.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of(startsToday));
        when(contracts.findByWarehouseIdAndStatus(2L, RentalStatus.ACTIVE)).thenReturn(List.of(startsToday));

        service.completeExpiredRentals(TODAY);

        assertThat(startsToday.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(south.getStatus()).isEqualTo(WarehouseStatus.RENTED);
        verify(warehouses).save(south);
    }

    @Test
    void warehouseUnderMaintenanceKeepsItsStatus() {
        Warehouse east = warehouse(3L, "Склад Изток", WarehouseStatus.MAINTENANCE);
        RentalContract expired = contract(east, TODAY.minusMonths(1), TODAY.minusDays(3));

        when(contracts.findByStatusAndEndDateBefore(RentalStatus.ACTIVE, TODAY)).thenReturn(List.of(expired));
        when(contracts.findByStatus(RentalStatus.ACTIVE)).thenReturn(List.of());

        service.completeExpiredRentals(TODAY);

        assertThat(expired.getStatus()).isEqualTo(RentalStatus.COMPLETED);
        assertThat(east.getStatus()).isEqualTo(WarehouseStatus.MAINTENANCE); // ремонтът не се „отменя“
    }
}
