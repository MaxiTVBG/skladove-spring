package bg.skladove.api.dto;

import bg.skladove.ai.WarehouseMatch;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Препоръчан склад (AI препоръка на склад)")
public record WarehouseMatchDto(
        WarehouseDto warehouse,
        @Schema(description = "Съвпадение с изискванията (0–100)", example = "88") int matchPercent,
        @Schema(description = "Кое пасва") List<String> pros,
        @Schema(description = "Кое не пасва напълно") List<String> cons,
        @Schema(description = "AI оценка на пазарната месечна цена в лв.", example = "1950") BigDecimal estimatedPrice,
        @Schema(description = "С колко % обявената цена е над (+) или под (−) оценката", example = "-8")
        Integer priceVsEstimatePercent,
        @Schema(description = "Обявената цена е поне 5% под оценката") boolean goodDeal) {

    public static WarehouseMatchDto from(WarehouseMatch m) {
        return new WarehouseMatchDto(WarehouseDto.from(m.warehouse()), m.matchPercent(), m.pros(), m.cons(),
                m.estimatedPrice(), m.priceVsEstimatePercent(), m.isGoodDeal());
    }
}
