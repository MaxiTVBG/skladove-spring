/**
 * Функционалност с изкуствен интелект (задача №15): препоръка за месечна цена на склад
 * с метода на k-те най-близки съседи (k-NN регресия).
 *
 * <ul>
 *   <li>{@link bg.skladove.ai.KnnPriceModel} — самият модел и проверката му (leave-one-out);</li>
 *   <li>{@link bg.skladove.ai.PriceRecommendationService} — събира данните от базата и примерния набор;</li>
 *   <li>{@link bg.skladove.ai.PriceRecommendationController} — JSON за бутона във формата за склад;</li>
 *   <li>{@link bg.skladove.ai.PriceModelController} — страница с резултатите от проверката.</li>
 * </ul>
 */
package bg.skladove.ai;
