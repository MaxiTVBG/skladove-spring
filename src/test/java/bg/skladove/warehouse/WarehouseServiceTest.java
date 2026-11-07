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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за WarehouseService (задачи №10 и №11).
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
    private AppUser agentPetar;
    private AppUser agentGeorgi;

    @BeforeEach
    void setUp() {
        owner = user(1L, "ivan", Role.OWNER);
        otherOwner = user(2L, "maria", Role.OWNER);
        agentPetar = user(5L, "petar", Role.AGENT);
        agentGeorgi = user(6L, "georgi", Role.AGENT);
    }

    /** AppUser няма setId (id-то се генерира от базата), затова го задаваме с reflection. */
    private static AppUser user(Long id, String username, Role role) {
        AppUser u = new AppUser(username, "hash", username, role);
        ReflectionTestUtils.setField(u, "id", id);
        return u;
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

    // ---------------- Задача №10: добавяне и редакция ----------------

    @Test
    void createSetsOwnerAndAvailableStatus() {
        when(users.findByUsername("ivan")).thenReturn(Optional.of(owner));
        when(warehouses.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        Warehouse created = service.create(validForm(), "ivan");

        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getStatus()).isEqualTo(WarehouseStatus.AVAILABLE);
        assertThat(created.getName()).isEqualTo("Склад Север"); // интервалите са махнати
        assertThat(created.getArea()).isEqualTo(250.0);
        assertThat(created.getAgents()).isEmpty();
    }

    @Test
    void agentCannotCreateWarehouse() {
        when(users.findByUsername("petar")).thenReturn(Optional.of(agentPetar));

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

    // ---------------- Задача №11: избор на складови агенти ----------------

    @Test
    void createAssignsSelectedAgents() {
        when(users.findByUsername("ivan")).thenReturn(Optional.of(owner));
        when(users.findAllById(Set.of(5L, 6L))).thenReturn(List.of(agentPetar, agentGeorgi));
        when(warehouses.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseForm form = validForm();
        form.setAgentIds(Set.of(5L, 6L));
        Warehouse created = service.create(form, "ivan");

        assertThat(created.getAgents()).containsExactlyInAnyOrder(agentPetar, agentGeorgi);
    }

    @Test
    void ownerCannotBeAssignedAsAgent() {
        when(users.findByUsername("ivan")).thenReturn(Optional.of(owner));
        when(users.findAllById(Set.of(2L))).thenReturn(List.of(otherOwner));

        WarehouseForm form = validForm();
        form.setAgentIds(Set.of(2L)); // id на собственик, подаден ръчно във формата

        assertThatThrownBy(() -> service.create(form, "ivan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("не е складов агент");
        verify(warehouses, never()).save(any());
    }

    @Test
    void unknownAgentIsRejected() {
        when(users.findByUsername("ivan")).thenReturn(Optional.of(owner));
        when(users.findAllById(Set.of(99L))).thenReturn(List.of());

        WarehouseForm form = validForm();
        form.setAgentIds(Set.of(99L));

        assertThatThrownBy(() -> service.create(form, "ivan"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Някой от избраните агенти не съществува.");
    }

    @Test
    void updateReplacesAgents() {
        Warehouse existing = new Warehouse();
        existing.setOwner(owner);
        existing.setAgents(new HashSet<>(Set.of(agentPetar)));
        when(warehouses.findById(3L)).thenReturn(Optional.of(existing));
        when(users.findAllById(Set.of(6L))).thenReturn(List.of(agentGeorgi));
        when(warehouses.save(any(Warehouse.class))).thenAnswer(inv -> inv.getArgument(0));

        WarehouseForm form = validForm();
        form.setAgentIds(Set.of(6L)); // махаме Петър, слагаме Георги
        Warehouse updated = service.update(3L, form, "ivan");

        assertThat(updated.getAgents()).containsExactly(agentGeorgi);
    }

    @Test
    void editFormIsPrefilledWithCurrentAgents() {
        Warehouse existing = new Warehouse();
        existing.setOwner(owner);
        existing.setName("Склад Юг");
        existing.setAddress("Русе");
        existing.setAgents(new HashSet<>(Set.of(agentPetar, agentGeorgi)));

        WarehouseForm form = WarehouseForm.from(existing);

        assertThat(form.getAgentIds()).containsExactlyInAnyOrder(5L, 6L);
    }
}
