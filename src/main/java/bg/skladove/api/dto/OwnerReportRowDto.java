package bg.skladove.api.dto;

import bg.skladove.report.OwnerWarehouseReportRow;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Ред от справката за складовете на собственика (справка №14)")
public record OwnerReportRowDto(
        WarehouseDto warehouse,
        @Schema(description = "Активни и приключили договори, които се застъпват с периода")
        List<RentalContractDto> contracts,
        @Schema(description = "Наети дни в периода (застъпванията се броят веднъж)", example = "21")
        long rentedDays,
        @Schema(description = "Дни в периода", example = "30") long periodDays,
        @Schema(description = "Заетост в %", example = "70") int occupancyPercent,
        @Schema(description = "Сума по договорите в лв.", example = "2500.00") BigDecimal contractsTotal) {

    public static OwnerReportRowDto from(OwnerWarehouseReportRow r) {
        return new OwnerReportRowDto(WarehouseDto.from(r.getWarehouse()),
                r.getContracts().stream().map(RentalContractDto::from).toList(),
                r.getRentedDays(), r.getPeriodDays(), r.getOccupancyPercent(), r.getContractsTotal());
    }
}
