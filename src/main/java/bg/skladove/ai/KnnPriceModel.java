package bg.skladove.ai;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Препоръка за цена с метода на k-те най-близки съседи (k-NN регресия).
 *
 * Идея: подобни складове имат подобна цена. За нов склад се намират k-те
 * най-подобни от данните и цената му се изчислява като среднопретеглена
 * от техните цени — по-подобните тежат повече.
 *
 * Моделът прогнозира цена на м², а не цялата цена. Така склад от 50 м² и склад
 * от 500 м² с еднакви условия могат да бъдат „съседи“, а накрая цената на м²
 * се умножава по площта на новия склад.
 *
 * Разстоянието между два склада се смята по четири признака, всеки сведен до 0..1:
 * <ul>
 *   <li>площ — по логаритъм, защото разликата 50→100 м² е по-съществена от 1000→1050 м²;</li>
 *   <li>височина — линейно спрямо обхвата в данните;</li>
 *   <li>категория — 0 ако е еднаква, 1 ако е различна;</li>
 *   <li>климат — разликата между класовете (сух, отопляем, хладилен, фризер), делена на 3.</li>
 * </ul>
 * Признакът, който липсва при единия склад, се брои като средно отдалечен (0.5).
 *
 * Класът не зависи от Spring и базата, затова се тества с обикновени JUnit тестове.
 */
public final class KnnPriceModel {

    /** Брой съседи; избран с проверката leave-one-out (страница „Как работи препоръката“). */
    public static final int DEFAULT_K = 3;

    // Тегла на признаците: колко силно всеки влияе на разстоянието.
    // Климатът и категорията тежат най-много, защото най-силно променят цената на м²;
    // площта вече е отчетена чрез цената на м², затова тежи по-малко.
    // Стойностите са избрани чрез сравнение на няколко варианта с leave-one-out.
    static final double WEIGHT_AREA = 1.0;
    static final double WEIGHT_HEIGHT = 0.3;
    static final double WEIGHT_CATEGORY = 2.0;
    static final double WEIGHT_CLIMATE = 4.0;

    private static final double MAX_DISTANCE =
            Math.sqrt(WEIGHT_AREA + WEIGHT_HEIGHT + WEIGHT_CATEGORY + WEIGHT_CLIMATE);
    private static final double UNKNOWN = 0.5;
    // Добавя се към разстоянието, за да няма делене на нула при съвпадение
    private static final double SMOOTHING = 0.05;

    private final List<PriceSample> training;
    private final double minLogArea;
    private final double maxLogArea;
    private final double minHeight;
    private final double maxHeight;

    public KnnPriceModel(List<PriceSample> training) {
        if (training == null || training.isEmpty()) {
            throw new IllegalArgumentException("Няма данни за обучение на модела.");
        }
        this.training = List.copyOf(training);

        double minLog = Double.MAX_VALUE, maxLog = -Double.MAX_VALUE;
        double minH = Double.MAX_VALUE, maxH = -Double.MAX_VALUE;
        for (PriceSample s : training) {
            double log = Math.log(s.features().area());
            minLog = Math.min(minLog, log);
            maxLog = Math.max(maxLog, log);
            Double h = s.features().height();
            if (h != null) {
                minH = Math.min(minH, h);
                maxH = Math.max(maxH, h);
            }
        }
        this.minLogArea = minLog;
        this.maxLogArea = maxLog;
        this.minHeight = minH;
        this.maxHeight = maxH;
    }

    public int size() {
        return training.size();
    }

    /** Препоръчва месечна цена за склад с дадените характеристики. */
    public PriceRecommendation recommend(PriceFeatures query, int k) {
        if (query.area() == null || query.area() <= 0) {
            throw new IllegalArgumentException("Въведете площ, за да се изчисли цена.");
        }

        List<PriceSample> nearest = nearest(query, k);

        double weightSum = 0;
        double weightedPricePerSqm = 0;
        double minPricePerSqm = Double.MAX_VALUE;
        double maxPricePerSqm = 0;
        List<PriceRecommendation.Neighbor> neighbors = new ArrayList<>();
        for (PriceSample s : nearest) {
            double d = distance(query, s.features());
            double weight = 1.0 / (d + SMOOTHING);
            double ppm = s.pricePerSquareMeter();
            weightSum += weight;
            weightedPricePerSqm += weight * ppm;
            minPricePerSqm = Math.min(minPricePerSqm, ppm);
            maxPricePerSqm = Math.max(maxPricePerSqm, ppm);
            neighbors.add(new PriceRecommendation.Neighbor(
                    s.source().getLabel(),
                    s.features().area(),
                    s.features().height(),
                    s.features().category(),
                    s.features().climate() != null ? s.features().climate().getLabel() : null,
                    s.monthlyPrice(),
                    money(ppm),
                    similarityPercent(d)));
        }
        double pricePerSqm = weightedPricePerSqm / weightSum;
        double area = query.area();

        return new PriceRecommendation(
                wholeLeva(pricePerSqm * area),
                wholeLeva(minPricePerSqm * area),
                wholeLeva(maxPricePerSqm * area),
                money(pricePerSqm),
                neighbors,
                training.size());
    }

    /** Само числото на препоръчаната цена (използва се при оценката на модела). */
    double predict(PriceFeatures query, int k) {
        double weightSum = 0;
        double weighted = 0;
        for (PriceSample s : nearest(query, k)) {
            double weight = 1.0 / (distance(query, s.features()) + SMOOTHING);
            weightSum += weight;
            weighted += weight * s.pricePerSquareMeter();
        }
        return weighted / weightSum * query.area();
    }

    private List<PriceSample> nearest(PriceFeatures query, int k) {
        return training.stream()
                .sorted(Comparator.comparingDouble(s -> distance(query, s.features())))
                .limit(Math.max(1, k))
                .toList();
    }

    /** Разстояние между два склада: 0 = еднакви, MAX_DISTANCE = възможно най-различни. */
    double distance(PriceFeatures a, PriceFeatures b) {
        double area = scaled(Math.log(a.area()), Math.log(b.area()), minLogArea, maxLogArea);

        double height = (a.height() == null || b.height() == null || maxHeight < minHeight)
                ? UNKNOWN
                : scaled(a.height(), b.height(), minHeight, maxHeight);

        double category = (a.category() == null || b.category() == null)
                ? UNKNOWN
                : (a.category().equalsIgnoreCase(b.category()) ? 0 : 1);

        double climate = (a.climate() == null || b.climate() == null)
                ? UNKNOWN
                : Math.abs(a.climate().ordinal() - b.climate().ordinal()) / 3.0;

        return Math.sqrt(WEIGHT_AREA * area * area
                + WEIGHT_HEIGHT * height * height
                + WEIGHT_CATEGORY * category * category
                + WEIGHT_CLIMATE * climate * climate);
    }

    /** Разлика между две стойности, сведена до 0..1 спрямо обхвата в данните. */
    private static double scaled(double x, double y, double min, double max) {
        double range = max - min;
        if (range <= 0) {
            return 0;
        }
        return Math.min(1.0, Math.abs(x - y) / range);
    }

    static int similarityPercent(double distance) {
        return (int) Math.round(100 * (1 - distance / MAX_DISTANCE));
    }

    /**
     * Проверка на модела с leave-one-out: за всеки склад моделът се обучава
     * на всички останали и прогнозира неговата цена. Сравнява се с базов метод,
     * който не гледа нищо освен площта: медианната цена на м² × площ.
     */
    public static ModelEvaluation evaluate(List<PriceSample> samples, int k) {
        if (samples.size() < 2) {
            throw new IllegalArgumentException("За проверка са нужни поне 2 склада.");
        }
        double errorSum = 0, percentSum = 0, baselineErrorSum = 0, baselinePercentSum = 0;
        for (int i = 0; i < samples.size(); i++) {
            PriceSample hidden = samples.get(i);
            List<PriceSample> rest = new ArrayList<>(samples);
            rest.remove(i);

            double actual = hidden.monthlyPrice().doubleValue();
            double predicted = new KnnPriceModel(rest).predict(hidden.features(), k);
            double baseline = medianPricePerSqm(rest) * hidden.features().area();

            errorSum += Math.abs(predicted - actual);
            percentSum += Math.abs(predicted - actual) / actual * 100;
            baselineErrorSum += Math.abs(baseline - actual);
            baselinePercentSum += Math.abs(baseline - actual) / actual * 100;
        }
        int n = samples.size();
        return new ModelEvaluation(k, n, errorSum / n, percentSum / n,
                baselineErrorSum / n, baselinePercentSum / n);
    }

    private static double medianPricePerSqm(List<PriceSample> samples) {
        double[] values = samples.stream().mapToDouble(PriceSample::pricePerSquareMeter).sorted().toArray();
        int mid = values.length / 2;
        return values.length % 2 == 1 ? values[mid] : (values[mid - 1] + values[mid]) / 2;
    }

    private static BigDecimal wholeLeva(double value) {
        return BigDecimal.valueOf(value).setScale(0, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
