package bg.skladove.ai;

import bg.skladove.warehouse.Warehouse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit тестове за оценката на съвпадение между склад и изискванията на клиента (задача №15).
 */
class WarehouseScorerTest {

    private static final LocalDate FROM = LocalDate.of(2026, 12, 1);
    private static final LocalDate TO = LocalDate.of(2026, 12, 31);

    private static Warehouse warehouse(double area, String climate, String goods, String category, String price) {
        Warehouse w = new Warehouse();
        w.setArea(area);
        w.setClimateConditions(climate);
        w.setGoodsType(goods);
        w.setCategory(category);
        w.setMonthlyPrice(new BigDecimal(price));
        return w;
    }

    private static WarehouseRequest request(double area, ClimateClass climate, String goods, String category, String budget) {
        return new WarehouseRequest(area, climate, goods, category,
                budget == null ? null : new BigDecimal(budget), FROM, TO);
    }

    @Test
    void perfectWarehouseScoresHundred() {
        WarehouseScorer.Score s = WarehouseScorer.score(
                request(100, ClimateClass.COOLED, "хранителни продукти", "A", "2000"),
                warehouse(100, "Хладилен (0–8 °C)", "Хранителни продукти и напитки", "A", "1800"));

        assertThat(s.percent()).isEqualTo(100);
        assertThat(s.suitable()).isTrue();
        assertThat(s.cons()).isEmpty();
        assertThat(s.pros()).contains("Подходящ климат: Хладилен", "В рамките на бюджета");
    }

    @Test
    void frozenGoodsInDryWarehouseAreNotSuitable() {
        WarehouseScorer.Score s = WarehouseScorer.score(
                request(100, ClimateClass.FROZEN, null, null, null),
                warehouse(100, "Сух", null, "B", "800"));

        assertThat(s.suitable()).isFalse();
    }

    @Test
    void tooSmallWarehouseIsPenalisedMoreThanTooBig() {
        double tooSmall = WarehouseScorer.areaScore(100, 50.0);   // половината от нужното
        double tooBig = WarehouseScorer.areaScore(100, 200.0);    // двойно повече

        assertThat(WarehouseScorer.areaScore(100, 100.0)).isEqualTo(1.0);
        assertThat(tooBig).isGreaterThan(tooSmall);
        assertThat(tooSmall).isLessThan(0.2);
    }

    @Test
    void priceOverBudgetLowersTheScoreAndIsExplained() {
        WarehouseScorer.Score s = WarehouseScorer.score(
                request(100, null, null, null, "1000"),
                warehouse(100, "Сух", null, "B", "1200"));

        assertThat(WarehouseScorer.budgetScore(new BigDecimal("1000"), new BigDecimal("1200"))).isEqualTo(0.6, org.assertj.core.data.Offset.offset(1e-9));
        assertThat(WarehouseScorer.budgetScore(new BigDecimal("1000"), new BigDecimal("1600"))).isZero();
        assertThat(s.cons()).contains("Над бюджета с 200 лв.");
        assertThat(s.percent()).isLessThan(100);
    }

    @Test
    void goodsTypeMatchesSimilarWords() {
        assertThat(WarehouseScorer.goodsScore("храни", "Хранителни стоки")).isEqualTo(1.0);
        assertThat(WarehouseScorer.goodsScore("мебели", "Хранителни стоки")).isZero();
        assertThat(WarehouseScorer.goodsScore(null, "Хранителни стоки")).isEqualTo(1.0);
    }

    @Test
    void acceptableButDifferentClimateIsAWarning() {
        WarehouseScorer.Score s = WarehouseScorer.score(
                request(100, ClimateClass.COOLED, null, null, null),
                warehouse(100, "Фризер (под -18 °C)", null, "B", "3000"));

        assertThat(s.suitable()).isTrue();
        assertThat(s.cons()).contains("Климатът е различен (Фризер), но е приемлив");
    }
}
