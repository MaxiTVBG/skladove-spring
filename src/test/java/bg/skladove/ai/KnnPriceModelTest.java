package bg.skladove.ai;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit тестове за модела k-NN (задача №15).
 * Моделът е обикновен Java клас, затова тестовете не стартират Spring.
 */
class KnnPriceModelTest {

    private static PriceSample sample(double area, double height, String category, ClimateClass climate, int price) {
        return new PriceSample(null, new PriceFeatures(area, height, category, climate),
                BigDecimal.valueOf(price), PriceSample.Source.REFERENCE);
    }

    @Test
    void identicalWarehouseGivesItsOwnPrice() {
        KnnPriceModel model = new KnnPriceModel(List.of(
                sample(100, 6, "B", ClimateClass.DRY, 800),
                sample(100, 6, "B", ClimateClass.FROZEN, 3000)));

        PriceRecommendation r = model.recommend(new PriceFeatures(100.0, 6.0, "B", ClimateClass.DRY), 1);

        assertThat(r.recommendedPrice()).isEqualByComparingTo("800");
        assertThat(r.neighbors()).hasSize(1);
        assertThat(r.neighbors().get(0).similarityPercent()).isEqualTo(100);
    }

    @Test
    void priceIsScaledByArea() {
        // Всички примери са по 10 лв./м²; за 250 м² очакваме 2500 лв.
        KnnPriceModel model = new KnnPriceModel(List.of(
                sample(50, 5, "B", ClimateClass.DRY, 500),
                sample(100, 6, "B", ClimateClass.DRY, 1000),
                sample(400, 8, "B", ClimateClass.DRY, 4000)));

        PriceRecommendation r = model.recommend(new PriceFeatures(250.0, 7.0, "B", ClimateClass.DRY), 3);

        assertThat(r.pricePerSquareMeter()).isEqualByComparingTo("10.00");
        assertThat(r.recommendedPrice()).isEqualByComparingTo("2500");
    }

    @Test
    void climateMattersMoreThanSize() {
        // Сухи складове ~8 лв./м², фризери ~30 лв./м². Фризер от 150 м² трябва да се сравни с фризерите.
        KnnPriceModel model = new KnnPriceModel(List.of(
                sample(140, 6, "B", ClimateClass.DRY, 1120),
                sample(160, 6, "B", ClimateClass.DRY, 1280),
                sample(150, 6, "B", ClimateClass.DRY, 1200),
                sample(60, 5, "B", ClimateClass.FROZEN, 1800),
                sample(300, 7, "B", ClimateClass.FROZEN, 9000),
                sample(500, 8, "B", ClimateClass.FROZEN, 15000)));

        PriceRecommendation r = model.recommend(new PriceFeatures(150.0, 6.0, "B", ClimateClass.FROZEN), 3);

        assertThat(r.neighbors()).allMatch(n -> "Фризер".equals(n.climate()));
        assertThat(r.recommendedPrice()).isEqualByComparingTo("4500"); // 30 лв./м² × 150 м²
    }

    @Test
    void recommendedPriceIsInsideTheRange() {
        KnnPriceModel model = new KnnPriceModel(List.of(
                sample(80, 5, "A", ClimateClass.HEATED, 1100),
                sample(120, 6, "A", ClimateClass.HEATED, 1300),
                sample(100, 5, "B", ClimateClass.HEATED, 900)));

        PriceRecommendation r = model.recommend(new PriceFeatures(100.0, 5.5, "A", ClimateClass.HEATED), 3);

        assertThat(r.recommendedPrice()).isBetween(r.minPrice(), r.maxPrice());
        assertThat(r.trainingSize()).isEqualTo(3);
    }

    @Test
    void missingFeaturesStillGiveARecommendation() {
        KnnPriceModel model = new KnnPriceModel(List.of(
                sample(100, 6, "B", ClimateClass.DRY, 800),
                sample(200, 7, "C", ClimateClass.HEATED, 1700)));

        PriceRecommendation r = model.recommend(new PriceFeatures(150.0, null, null, null), 2);

        assertThat(r.recommendedPrice()).isPositive();
    }

    @Test
    void areaIsRequired() {
        KnnPriceModel model = new KnnPriceModel(List.of(sample(100, 6, "B", ClimateClass.DRY, 800)));

        assertThatThrownBy(() -> model.recommend(new PriceFeatures(null, 6.0, "B", ClimateClass.DRY), 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void emptyTrainingDataIsRejected() {
        assertThatThrownBy(() -> new KnnPriceModel(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Основният тест за качеството: върху примерния набор моделът трябва да греши
     * значително по-малко от базовия метод, който гледа само площта.
     */
    @Test
    void modelIsMoreAccurateThanBaselineOnReferenceData() {
        List<PriceSample> data = ReferencePriceData.loadFromClasspath();

        ModelEvaluation e = KnnPriceModel.evaluate(data, KnnPriceModel.DEFAULT_K);

        System.out.printf("Leave-one-out върху %d склада, k=%d: грешка %.0f лв. (%.1f %%), базов метод %.0f лв. (%.1f %%)%n",
                e.sampleCount(), e.k(), e.meanAbsoluteError(), e.meanAbsolutePercentError(),
                e.baselineAbsoluteError(), e.baselineAbsolutePercentError());
        assertThat(e.sampleCount()).isEqualTo(data.size());
        assertThat(e.meanAbsolutePercentError()).isLessThan(15.0);
        assertThat(e.meanAbsoluteError()).isLessThan(e.baselineAbsoluteError() / 2);
    }
}
