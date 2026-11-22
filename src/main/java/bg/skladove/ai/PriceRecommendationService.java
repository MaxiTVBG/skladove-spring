package bg.skladove.ai;

import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Препоръка за месечна цена на склад (задача №15).
 *
 * Данните за обучение са примерният набор плюс всички складове в системата,
 * които имат площ и цена. При редакция самият склад се изключва, за да не
 * „подскаже“ собствената си цена.
 */
@Service
public class PriceRecommendationService {

    private static final Logger log = LogManager.getLogger(PriceRecommendationService.class);

    static final int MIN_TRAINING_SIZE = 3;
    static final int MAX_EVALUATED_K = 10;

    private final WarehouseRepository warehouses;
    private final ReferencePriceData reference;

    public PriceRecommendationService(WarehouseRepository warehouses, ReferencePriceData reference) {
        this.warehouses = warehouses;
        this.reference = reference;
    }

    @Transactional(readOnly = true)
    public PriceRecommendation recommend(PriceFeatures query, Long excludeWarehouseId) {
        if (query.area() == null || query.area() <= 0) {
            throw new IllegalArgumentException("Въведете площ, за да се изчисли цена.");
        }
        List<PriceSample> data = trainingData(excludeWarehouseId);
        if (data.size() < MIN_TRAINING_SIZE) {
            throw new IllegalStateException("Няма достатъчно данни за препоръка на цена.");
        }

        PriceRecommendation result = new KnnPriceModel(data).recommend(query, KnnPriceModel.DEFAULT_K);
        log.info("AI препоръка за цена: площ {} м², категория {}, климат {} -> {} лв. (от {} склада)",
                query.area(), query.category(), query.climate(), result.recommendedPrice(), data.size());
        return result;
    }

    /**
     * Оценка на пазарната цена на вече съществуващи складове (използва се в препоръката на склад).
     * Всеки склад се оценява от останалите данни, без собствената си цена.
     *
     * @return id на склада -> оценена месечна цена; складове без площ липсват в резултата
     */
    @Transactional(readOnly = true)
    public Map<Long, BigDecimal> estimateMarketPrices(List<Warehouse> targets) {
        List<PriceSample> all = trainingData(null);
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Warehouse w : targets) {
            if (w.getArea() == null || w.getArea() <= 0) {
                continue;
            }
            List<PriceSample> others = all.stream()
                    .filter(s -> s.warehouseId() == null || !s.warehouseId().equals(w.getId()))
                    .toList();
            if (others.size() < MIN_TRAINING_SIZE) {
                continue;
            }
            result.put(w.getId(), new KnnPriceModel(others)
                    .recommend(toFeatures(w), KnnPriceModel.DEFAULT_K)
                    .recommendedPrice());
        }
        return result;
    }

    /** Проверка на модела за k от 1 до 10 върху всички налични данни. */
    @Transactional(readOnly = true)
    public List<ModelEvaluation> evaluate() {
        List<PriceSample> data = trainingData(null);
        List<ModelEvaluation> result = new ArrayList<>();
        for (int k = 1; k <= Math.min(MAX_EVALUATED_K, data.size() - 1); k++) {
            result.add(KnnPriceModel.evaluate(data, k));
        }
        log.info("Проверка на модела за цена върху {} склада", data.size());
        return result;
    }

    @Transactional(readOnly = true)
    public List<PriceSample> trainingData(Long excludeWarehouseId) {
        List<PriceSample> data = new ArrayList<>(reference.samples());
        for (Warehouse w : warehouses.findAll()) {
            if (Objects.equals(w.getId(), excludeWarehouseId)) {
                continue;
            }
            if (w.getArea() == null || w.getArea() <= 0
                    || w.getMonthlyPrice() == null || w.getMonthlyPrice().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            data.add(new PriceSample(w.getId(), toFeatures(w), w.getMonthlyPrice(), PriceSample.Source.SYSTEM));
        }
        return data;
    }

    static PriceFeatures toFeatures(Warehouse w) {
        return new PriceFeatures(w.getArea(), w.getHeight(), w.getCategory(),
                ClimateClass.fromText(w.getClimateConditions()));
    }
}
