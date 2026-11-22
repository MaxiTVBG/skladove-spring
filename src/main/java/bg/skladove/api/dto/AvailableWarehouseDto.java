package bg.skladove.api.dto;

import bg.skladove.report.AvailableWarehouse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Свободен склад за избрания период (справка №13)")
public record AvailableWarehouseDto(
        WarehouseDto warehouse,
        @Schema(description = "Свободен от (денят след последния договор преди периода); null = без договор преди това")
        LocalDate freeSince,
        @Schema(description = "Свободен до (денят преди следващия договор); null = без договор след това")
        LocalDate freeUntil) {

    public static AvailableWarehouseDto from(AvailableWarehouse a) {
        return new AvailableWarehouseDto(WarehouseDto.from(a.getWarehouse()), a.getFreeSince(), a.getFreeUntil());
    }
}
