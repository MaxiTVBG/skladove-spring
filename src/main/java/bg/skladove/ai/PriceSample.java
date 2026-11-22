package bg.skladove.ai;

import java.math.BigDecimal;

/**
 * Един пример за обучение: характеристики на склад и неговата месечна цена.
 *
 * @param warehouseId id на склада в базата или null за примерните данни
 */
public record PriceSample(Long warehouseId, PriceFeatures features, BigDecimal monthlyPrice, Source source) {

    /** Откъде идва примерът. */
    public enum Source {
        REFERENCE("Примерни данни"),
        SYSTEM("Склад в системата");

        private final String label;

        Source(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    /** Цена на квадратен метър: това е стойността, която моделът реално прогнозира. */
    public double pricePerSquareMeter() {
        return monthlyPrice.doubleValue() / features.area();
    }
}
