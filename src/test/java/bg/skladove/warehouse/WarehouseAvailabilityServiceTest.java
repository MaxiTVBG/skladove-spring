package bg.skladove.warehouse;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за WarehouseAvailabilityService (задачи №10 и №12).
 */
@ExtendWith(MockitoExtension.class)
class WarehouseAvailabilityServiceTest {

    @Mock
    private RentalContractRepository contractRepository;

    @InjectMocks
    private WarehouseAvailabilityService availabilityService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        ReflectionTestUtils.setField(warehouse, "id", 1L);
        warehouse.setStatus(WarehouseStatus.AVAILABLE);
    }

    @Test
    void inactiveOrMaintenanceWarehouseIsNeverAvailable() {
        warehouse.setStatus(WarehouseStatus.MAINTENANCE);
        assertThat(availabilityService.isAvailable(
                warehouse, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10))).isFalse();

        warehouse.setStatus(WarehouseStatus.INACTIVE);
        assertThat(availabilityService.isAvailable(
                warehouse, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10))).isFalse();
    }

    @Test
    void detectsOverlappingActiveContracts() {
        RentalContract active = BeanUtils.instantiateClass(RentalContract.class);
        active.setStartDate(LocalDate.of(2026, 12, 10));
        active.setEndDate(LocalDate.of(2026, 12, 20));

        when(contractRepository.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(List.of(active));

        // Застъпване в края
        assertThat(availabilityService.isAvailable(
                warehouse, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10))).isFalse();
        // Преди договора -> свободен
        assertThat(availabilityService.isAvailable(
                warehouse, LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 9))).isTrue();
        // След договора -> свободен
        assertThat(availabilityService.isAvailable(
                warehouse, LocalDate.of(2026, 12, 21), LocalDate.of(2026, 12, 31))).isTrue();
    }

    @Test
    void synchronizeStatusUpdatesToRentedWhenActiveContractCoversToday() {
        LocalDate today = LocalDate.now();
        RentalContract current = BeanUtils.instantiateClass(RentalContract.class);
        current.setStartDate(today.minusDays(2));
        current.setEndDate(today.plusDays(5));

        when(contractRepository.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(List.of(current));

        availabilityService.synchronizeStatus(warehouse);
        assertThat(warehouse.getStatus()).isEqualTo(WarehouseStatus.RENTED);

        // Когато няма активен договор за днес -> AVAILABLE
        when(contractRepository.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE))
                .thenReturn(List.of());
        availabilityService.synchronizeStatus(warehouse);
        assertThat(warehouse.getStatus()).isEqualTo(WarehouseStatus.AVAILABLE);
    }

    @Test
    void synchronizeStatusDoesNotOverrideMaintenance() {
        warehouse.setStatus(WarehouseStatus.MAINTENANCE);
        availabilityService.synchronizeStatus(warehouse);
        assertThat(warehouse.getStatus()).isEqualTo(WarehouseStatus.MAINTENANCE);
    }
}
