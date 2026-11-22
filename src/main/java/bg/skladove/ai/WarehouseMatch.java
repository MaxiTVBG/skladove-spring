package bg.skladove.ai;

import bg.skladove.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Един препоръчан склад.
 *
 * @param matchPercent            доколко складът отговаря на изискванията (0–100)
 * @param pros                    кое пасва
 * @param cons                    кое не пасва напълно
 * @param estimatedPrice          AI оценка на пазарната месечна цена (null, ако няма площ)
 * @param priceVsEstimatePercent  с колко % обявената цена е над (+) или под (−) оценката
 */
public record WarehouseMatch(Warehouse warehouse,
                             int matchPercent,
                             List<String> pros,
                             List<String> cons,
                             BigDecimal estimatedPrice,
                             Integer priceVsEstimatePercent) {

    /** Обявената цена е поне 5% под пазарната оценка. */
    public boolean isGoodDeal() {
        return priceVsEstimatePercent != null && priceVsEstimatePercent <= -5;
    }
}
