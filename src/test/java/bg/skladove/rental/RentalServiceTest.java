package bg.skladove.rental;

import bg.skladove.notification.Notification;
import bg.skladove.notification.NotificationService;
import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.BeanUtils;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за RentalService (задача №12).
 */
@ExtendWith(MockitoExtension.class)
class RentalServiceTest {

    @Mock
    private RentalContractRepository contractRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private AppUserRepository userRepository;

    @Mock
    private WarehouseAvailabilityService availabilityService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private RentalService rentalService;

    private AppUser owner;
    private AppUser otherOwner;
    private AppUser agent;
    private AppUser unassignedAgent;
    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        owner = user(1L, "ivan", Role.OWNER);
        otherOwner = user(2L, "maria", Role.OWNER);
        agent = user(5L, "petar", Role.AGENT);
        unassignedAgent = user(6L, "georgi", Role.AGENT);

        warehouse = new Warehouse();
        ReflectionTestUtils.setField(warehouse, "id", 10L);
        warehouse.setName("Склад Запад");
        warehouse.setOwner(owner);
        warehouse.getAgents().add(agent);
    }

    private static AppUser user(Long id, String username, Role role) {
        AppUser u = new AppUser(username, "hash", username, role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
    }

    @Test
    void createRequestSavesPendingContractAndNotifiesAgent() {
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(5L)).thenReturn(Optional.of(agent));
        when(availabilityService.isAvailable(warehouse, start, end)).thenReturn(true);
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalContract saved = rentalService.createRequest(
                10L, 5L, "Фирма ЕООД", "0888123456", "office@firma.bg",
                start, end, new BigDecimal("1500.00"));

        assertThat(saved.getStatus()).isEqualTo(RentalStatus.PENDING_OWNER_APPROVAL);
        assertThat(saved.getTenantName()).isEqualTo("Фирма ЕООД");
        assertThat(saved.getWarehouse()).isSameAs(warehouse);
        assertThat(saved.getAgent()).isSameAs(agent);
        verify(notificationService).createNotification(
                eq(agent), eq(Notification.Type.NEW_RENTAL_REQUEST), contains("Склад Запад"));
    }

    @Test
    void createRequestRejectsNonAgentUser() {
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> rentalService.createRequest(
                10L, 1L, "Фирма", null, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10),
                new BigDecimal("500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Само агент");

        verify(contractRepository, never()).save(any());
    }

    @Test
    void createRequestRejectsUnassignedAgent() {
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(6L)).thenReturn(Optional.of(unassignedAgent));

        assertThatThrownBy(() -> rentalService.createRequest(
                10L, 6L, "Фирма", null, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10),
                new BigDecimal("500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не е назначен");

        verify(contractRepository, never()).save(any());
    }

    @Test
    void createRequestRejectsInvertedDatesAndNonPositivePrice() {
        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(5L)).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> rentalService.createRequest(
                10L, 5L, "Фирма", null, null,
                LocalDate.of(2026, 12, 10), LocalDate.of(2026, 12, 1),
                new BigDecimal("500")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Невалиден период");

        assertThatThrownBy(() -> rentalService.createRequest(
                10L, 5L, "Фирма", null, null,
                LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 10),
                BigDecimal.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("положителна");
    }

    @Test
    void createRequestFailsWhenWarehouseUnavailable() {
        LocalDate start = LocalDate.of(2026, 12, 1);
        LocalDate end = LocalDate.of(2026, 12, 31);

        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(userRepository.findById(5L)).thenReturn(Optional.of(agent));
        when(availabilityService.isAvailable(warehouse, start, end)).thenReturn(false);

        assertThatThrownBy(() -> rentalService.createRequest(
                10L, 5L, "Фирма", null, null, start, end, new BigDecimal("800")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("не е свободен");
    }

    @Test
    void approveRequestActivatesContractSynchronizesWarehouseAndNotifiesOwner() {
        RentalContract contract = pendingContract(100L);

        when(contractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(availabilityService.isAvailable(
                warehouse, contract.getStartDate(), contract.getEndDate())).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));

        RentalContract approved = rentalService.approveRequest(100L, 1L);

        assertThat(approved.getStatus()).isEqualTo(RentalStatus.ACTIVE);
        assertThat(approved.getDecidedBy()).isSameAs(owner);
        assertThat(approved.getDecidedAt()).isNotNull();
        verify(availabilityService).synchronizeStatus(warehouse);
        verify(warehouseRepository).save(warehouse);
        verify(notificationService).createNotification(
                eq(owner), eq(Notification.Type.WAREHOUSE_RENTED), contains("Склад Запад"));
    }

    @Test
    void approveRequestRejectsOtherOwnerOrAlreadyProcessed() {
        RentalContract contract = pendingContract(100L);
        when(contractRepository.findById(100L)).thenReturn(Optional.of(contract));

        assertThatThrownBy(() -> rentalService.approveRequest(100L, 2L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Нямате право");

        contract.setStatus(RentalStatus.ACTIVE);
        assertThatThrownBy(() -> rentalService.approveRequest(100L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("вече е обработена");
    }

    @Test
    void rejectRequestSetsRejectedStatusAndNote() {
        RentalContract contract = pendingContract(100L);
        when(contractRepository.findById(100L)).thenReturn(Optional.of(contract));
        when(userRepository.findById(1L)).thenReturn(Optional.of(owner));
        when(contractRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RentalContract rejected = rentalService.rejectRequest(100L, 1L, "Ниска цена");

        assertThat(rejected.getStatus()).isEqualTo(RentalStatus.REJECTED);
        assertThat(rejected.getDecisionNote()).isEqualTo("Ниска цена");
        assertThat(rejected.getDecidedBy()).isSameAs(owner);
        verify(notificationService, never()).createNotification(any(), any(), any());
    }

    private RentalContract pendingContract(Long id) {
        RentalContract c = BeanUtils.instantiateClass(RentalContract.class);
        ReflectionTestUtils.setField(c, "id", id);
        c.setWarehouse(warehouse);
        c.setAgent(agent);
        c.setTenantName("Клиент АД");
        c.setStartDate(LocalDate.of(2026, 12, 1));
        c.setEndDate(LocalDate.of(2026, 12, 31));
        c.setPrice(new BigDecimal("1000"));
        c.setStatus(RentalStatus.PENDING_OWNER_APPROVAL);
        return c;
    }
}
