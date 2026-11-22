package bg.skladove.ai;

import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit тестове за PriceRecommendationService: кои данни влизат в модела.
 * Mock-ва се само repository-то (интерфейс); примерните данни са подадени директно.
 */
@ExtendWith(MockitoExtension.class)
class PriceRecommendationServiceTest {

    @Mock
    private WarehouseRepository warehouses;

    private PriceRecommendationService service;

    @BeforeEach
    void setUp() {
        ReferencePriceData reference = ReferencePriceData.of(List.of(
                new PriceSample(null, new PriceFeatures(100.0, 6.0, "B", ClimateClass.DRY),
                        new BigDecimal("800"), PriceSample.Source.REFERENCE)));
        service = new PriceRecommendationService(warehouses, reference);
    }

    private static Warehouse warehouse(Long id, Double area, String price, String climate) {
        Warehouse w = new Warehouse();
        ReflectionTestUtils.setField(w, "id", id);
        w.setArea(area);
        w.setHeight(6.0);
        w.setCategory("B");
        w.setClimateConditions(climate);
        w.setMonthlyPrice(price == null ? null : new BigDecimal(price));
        return w;
    }

    @Test
    void trainingDataContainsReferenceAndSystemWarehouses() {
        when(warehouses.findAll()).thenReturn(List.of(
                warehouse(1L, 200.0, "1600", "Сух"),
                warehouse(2L, 50.0, "1500", "Фризер (под -18 °C)")));

        List<PriceSample> data = service.trainingData(null);

        assertThat(data).hasSize(3);
        assertThat(data).filteredOn(s -> s.source() == PriceSample.Source.SYSTEM)
                .extracting(PriceSample::warehouseId).containsExactly(1L, 2L);
        assertThat(data.get(2).features().climate()).isEqualTo(ClimateClass.FROZEN);
    }

    @Test
    void editedWarehouseAndWarehousesWithoutPriceAreSkipped() {
        when(warehouses.findAll()).thenReturn(List.of(
                warehouse(1L, 200.0, "1600", "Сух"),   // редактираният склад
                warehouse(2L, 80.0, null, "Сух"),      // без цена
                warehouse(3L, null, "900", "Сух"),     // без площ
                warehouse(4L, 120.0, "960", "Сух")));

        List<PriceSample> data = service.trainingData(1L);

        assertThat(data).extracting(PriceSample::warehouseId).containsExactly(null, 4L);
    }

    @Test
    void recommendUsesTheData() {
        when(warehouses.findAll()).thenReturn(List.of(
                warehouse(1L, 200.0, "1600", "Сух"),
                warehouse(2L, 50.0, "400", "Сух")));

        // всички складове са по 8 лв./м²
        PriceRecommendation r = service.recommend(new PriceFeatures(150.0, 6.0, "B", ClimateClass.DRY), null);

        assertThat(r.recommendedPrice()).isEqualByComparingTo("1200");
        assertThat(r.trainingSize()).isEqualTo(3);
    }

    @Test
    void tooLittleDataIsReported() {
        when(warehouses.findAll()).thenReturn(List.of());

        assertThatThrownBy(() -> service.recommend(new PriceFeatures(150.0, 6.0, "B", ClimateClass.DRY), null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Няма достатъчно данни за препоръка на цена.");
    }

    @Test
    void areaIsRequired() {
        assertThatThrownBy(() -> service.recommend(new PriceFeatures(null, 6.0, "B", ClimateClass.DRY), null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
