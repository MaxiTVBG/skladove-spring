package bg.skladove.api.dto;

import bg.skladove.user.AppUser;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

/**
 * Склад в отговорите на API-то.
 * Връща се DTO, а не самото entity: така не излизат LAZY връзки и данни на собственика.
 */
@Schema(description = "Склад")
public record WarehouseDto(
        @Schema(example = "1") Long id,
        @Schema(example = "Склад Север") String name,
        @Schema(example = "Варна, ул. Девня 12") String address,
        @Schema(description = "Площ в м²", example = "120") Double area,
        @Schema(description = "Височина в м", example = "6") Double height,
        @Schema(example = "Хладилен (0–8 °C)") String climateConditions,
        @Schema(example = "Хранителни продукти") String goodsType,
        @Schema(example = "B") String category,
        @Schema(description = "Месечна цена в лв.", example = "1800.00") BigDecimal monthlyPrice,
        WarehouseStatus status,
        @Schema(description = "Потребителски имена на складовите агенти; null, ако не са заредени")
        List<String> agents) {

    /** Без агентите (те са LAZY и не винаги са заредени). */
    public static WarehouseDto from(Warehouse w) {
        return of(w, null);
    }

    /** С агентите; използва се само когато са заредени в транзакцията. */
    public static WarehouseDto withAgents(Warehouse w) {
        return of(w, w.getAgents().stream()
                .map(AppUser::getUsername)
                .sorted(Comparator.naturalOrder())
                .toList());
    }

    private static WarehouseDto of(Warehouse w, List<String> agents) {
        return new WarehouseDto(w.getId(), w.getName(), w.getAddress(), w.getArea(), w.getHeight(),
                w.getClimateConditions(), w.getGoodsType(), w.getCategory(), w.getMonthlyPrice(),
                w.getStatus(), agents);
    }
}
