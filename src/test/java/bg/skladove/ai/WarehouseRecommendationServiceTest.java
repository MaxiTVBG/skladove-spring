package bg.skladove.ai;

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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за препоръката на склад (задача №15).
 * Mock-ват се само repository-тата (интерфейси); услугите за наличност и цена са истинските.
 */
@ExtendWith(MockitoExtension.class)
class WarehouseRecommendationServiceTest {

    private static final LocalDate FROM = LocalDate.of(2026, 12, 1);
    private static final LocalDate TO = LocalDate.of(2026, 12, 31);

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private AppUserRepository users;

    @Mock
    private RentalContractRepository contracts;

    private WarehouseRecommendationService service;
    private AppUser agent;

    @BeforeEach
    void setUp() {
        PriceRecommendationService prices = new PriceRecommendationService(warehouses,
                ReferencePriceData.of(ReferencePriceData.loadFromClasspath()));
        service = new WarehouseRecommendationService(warehouses, users,
                new WarehouseAvailabilityService(contracts), prices);
        agent = new AppUser("petar", "hash", "Петър", Role.AGENT);
        lenient().when(contracts.findByWarehouseIdAndStatus(anyLong(), eq(RentalStatus.ACTIVE))).thenReturn(List.of());
    }

    private static Warehouse warehouse(Long id, String name, double area, String climate, String price) {
        Warehouse w = new Warehouse();
        ReflectionTestUtils.setField(w, "id", id);
        w.setName(name);
        w.setArea(area);
        w.setHeight(6.0);
        w.setCategory("B");
        w.setClimateConditions(climate);
        w.setMonthlyPrice(new BigDecimal(price));
        w.setStatus(WarehouseStatus.AVAILABLE);
        return w;
    }

    private static WarehouseRequest request(double area, ClimateClass climate, String budget) {
        return new WarehouseRequest(area, climate, null, null,
                budget == null ? null : new BigDecimal(budget), FROM, TO);
    }

    @Test
    void bestMatchIsFirstAndUnsuitableClimateIsLeftOut() {
        Warehouse dry = warehouse(1L, "Сух", 100, "Сух", "800");
        Warehouse cold = warehouse(2L, "Хладилен", 100, "Хладилен (0–8 °C)", "1800");
        Warehouse frozen = warehouse(3L, "Фризер", 100, "Фризер (под -18 °C)", "2600");
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(dry, frozen, cold));
        when(warehouses.findAll()).thenReturn(List.of(dry, cold, frozen));

        List<WarehouseMatch> matches = service.recommend("petar", request(100, ClimateClass.COOLED, null));

        // хладилният е идеален, фризерът е приемлив, сухият отпада (стоката би се развалила)
        assertThat(matches).extracting(m -> m.warehouse().getName()).containsExactly("Хладилен", "Фризер");
        assertThat(matches.get(0).matchPercent()).isEqualTo(100);
    }

    @Test
    void rentedWarehouseInThePeriodIsNotRecommended() {
        Warehouse busy = warehouse(1L, "Зает", 100, "Сух", "800");
        Warehouse free = warehouse(2L, "Свободен", 100, "Сух", "850");
        RentalContract contract = BeanUtils.instantiateClass(RentalContract.class);
        contract.setWarehouse(busy);
        contract.setStartDate(LocalDate.of(2026, 11, 15));
        contract.setEndDate(LocalDate.of(2026, 12, 15));
        contract.setStatus(RentalStatus.ACTIVE);
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(busy, free));
        when(warehouses.findAll()).thenReturn(List.of(busy, free));
        when(contracts.findByWarehouseIdAndStatus(1L, RentalStatus.ACTIVE)).thenReturn(List.of(contract));

        List<WarehouseMatch> matches = service.recommend("petar", request(100, ClimateClass.DRY, null));

        assertThat(matches).extracting(m -> m.warehouse().getName()).containsExactly("Свободен");
    }

    @Test
    void marketPriceEstimateIsAdded() {
        Warehouse cheap = warehouse(1L, "Евтин", 100, "Сух", "500");
        when(users.findByUsername("petar")).thenReturn(Optional.of(agent));
        when(warehouses.findByAgentsContaining(agent)).thenReturn(List.of(cheap));
        when(warehouses.findAll()).thenReturn(List.of(cheap));

        WarehouseMatch match = service.recommend("petar", request(100, ClimateClass.DRY, null)).get(0);

        // в примерните данни сух склад от 100 м² е около 900–1000 лв., значи 500 лв. е изгодно
        assertThat(match.estimatedPrice()).isNotNull();
        assertThat(match.priceVsEstimatePercent()).isNegative();
        assertThat(match.isGoodDeal()).isTrue();
    }

    @Test
    void ownerCannotUseTheRecommendation() {
        AppUser owner = new AppUser("mario", "hash", "Марио", Role.OWNER);
        when(users.findByUsername("mario")).thenReturn(Optional.of(owner));

        assertThatThrownBy(() -> service.recommend("mario", request(100, null, null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void areaAndPeriodAreValidated() {
        assertThatThrownBy(() -> service.recommend("petar", request(0, null, null)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Въведете нужната площ.");
        assertThatThrownBy(() -> service.recommend("petar",
                new WarehouseRequest(100.0, null, null, null, null, TO, FROM)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void percentDifferenceIsSigned() {
        assertThat(WarehouseRecommendationService.percentDifference(new BigDecimal("900"), new BigDecimal("1000"))).isEqualTo(-10);
        assertThat(WarehouseRecommendationService.percentDifference(new BigDecimal("1150"), new BigDecimal("1000"))).isEqualTo(15);
        assertThat(WarehouseRecommendationService.percentDifference(new BigDecimal("900"), null)).isNull();
    }
}
