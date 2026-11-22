package bg.skladove.ai;

/**
 * Характеристиките на склад, от които моделът изчислява цената.
 * Височината, категорията и климатът може да липсват (null).
 */
public record PriceFeatures(Double area, Double height, String category, ClimateClass climate) {
}
