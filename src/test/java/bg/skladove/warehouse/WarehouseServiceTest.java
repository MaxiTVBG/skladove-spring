package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за WarehouseService.
 * Repository-тата са заменени с Mockito "mock"-ове, затова тестовете
 * не ползват база данни и вървят за части от секундата.
 */
@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private AppUserRepository users;

    @InjectMocks
    private WarehouseService service;

    private AppUser owner;
    private AppUser otherOwner;

    @BeforeEach
    void setUp() {
        owner = new AppUser("ivan", "hash", "Иван Петров", Role.OWNER);
        otherOwner = new AppUser("maria", "hash", "Мария Иванова", Role.OWNER);
    }

    private WarehouseForm validForm() {
        WarehouseForm form = new WarehouseForm();
        form.setName("  Склад Север  ");
        form.setAddress("Варна, ул. Морска 5");
        form.setArea(250.0);
        form.setHeight(6.0);
        form.setCategory("A");
        form.setMonthlyPrice(new BigDecimal("1200.00"));
        return form;
    }

    @Test
    void createSetsOwnerAndAvailableStatus() {
        when(users.findByUsername("ivan")).thenReturn(Optional.of(owner));
        when(warehouses.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        Warehouse created = service.create(validForm(), "ivan");

        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getStatus()).isEqualTo(WarehouseStatus.AVAILABLE);
        assertThat(created.getName()).isEqualTo("Склад Север"); // интервалите са махнати
        assertThat(created.getArea()).isEqualTo(250.0);
    }

    @Test
    void agentCannotCreateWarehouse() {
        AppUser agent = new AppUser("petar", "hash", "Петър", Role.AGENT);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));

        assertThatThrownBy(() -> service.create(validForm(), "petar"))
                .isInstanceOf(AccessDeniedException.class);
        verify(warehouses, never()).save(any());
    }

    @Test
    void ownerCannotOpenSomeoneElsesWarehouse() {
        Warehouse foreign = new Warehouse();
        foreign.setOwner(otherOwner);
        when(warehouses.findById(7L)).thenReturn(Optional.of(foreign));

        assertThatThrownBy(() -> service.getOwnedWarehouse(7L, "ivan"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void updateChangesFieldsButKeepsOwnerAndStatus() {
        Warehouse existing = new Warehouse();
        existing.setOwner(owner);
        existing.setStatus(WarehouseStatus.RENTED);
        when(warehouses.findById(3L)).thenReturn(Optional.of(existing));
        when(warehouses.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseForm form = validForm();
        form.setMonthlyPrice(new BigDecimal("1500.00"));
        Warehouse updated = service.update(3L, form, "ivan");

        assertThat(updated.getMonthlyPrice()).isEqualByComparingTo("1500.00");
        assertThat(updated.getOwner()).isSameAs(owner);
        assertThat(updated.getStatus()).isEqualTo(WarehouseStatus.RENTED); // редакцията не пипа статуса
    }

    @Test
    void missingWarehouseGivesClearMessage() {
        when(warehouses.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOwnedWarehouse(99L, "ivan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Складът не е намерен.");
    }
}
