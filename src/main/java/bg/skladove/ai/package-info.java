/**
 * Функционалност с изкуствен интелект (задача №15):
 * <ol>
 *   <li>препоръка за месечна цена на склад за собственика — k-NN регресия;</li>
 *   <li>препоръка на склад за клиент за агента — оценка на съвпадение по изискванията
 *       плюс пазарна оценка на цената от модела в т. 1.</li>
 * </ol>
 *
 * <ul>
 *   <li>{@link bg.skladove.ai.KnnPriceModel} — самият модел и проверката му (leave-one-out);</li>
 *   <li>{@link bg.skladove.ai.PriceRecommendationService} — събира данните от базата и примерния набор;</li>
 *   <li>{@link bg.skladove.ai.PriceRecommendationController} — JSON за бутона във формата за склад;</li>
 *   <li>{@link bg.skladove.ai.PriceModelController} — страница с резултатите от проверката;</li>
 *   <li>{@link bg.skladove.ai.WarehouseScorer} — оценка доколко склад отговаря на изискванията;</li>
 *   <li>{@link bg.skladove.ai.WarehouseRecommendationService} и
 *       {@link bg.skladove.ai.WarehouseRecommendationController} — препоръка на склад за агента.</li>
 * </ul>
 */
package bg.skladove.ai;
