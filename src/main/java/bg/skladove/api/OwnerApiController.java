package bg.skladove.api;

import bg.skladove.api.dto.ApiError;
import bg.skladove.api.dto.OwnerReportRowDto;
import bg.skladove.api.dto.RentalContractDto;
import bg.skladove.api.dto.WarehouseDto;
import bg.skladove.rental.RentalQueryService;
import bg.skladove.report.OwnerWarehouseReportService;
import bg.skladove.warehouse.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API за собственика (само четене). Адресите са под /owner/**, затова са достъпни само за роля OWNER.
 * Логиката е същата като в HTML страниците — контролерът само вика вече съществуващите услуги.
 */
@RestController
@RequestMapping("/owner/api")
@Tag(name = "Собственик", description = "Складове, заявки за наем и справка за собственика (роля OWNER)")
public class OwnerApiController {

    private final WarehouseService warehouseService;
    private final RentalQueryService rentalQueries;
    private final OwnerWarehouseReportService ownerReport;

    public OwnerApiController(WarehouseService warehouseService,
                              RentalQueryService rentalQueries,
                              OwnerWarehouseReportService ownerReport) {
        this.warehouseService = warehouseService;
        this.rentalQueries = rentalQueries;
        this.ownerReport = ownerReport;
    }

    @GetMapping("/warehouses")
    @Operation(summary = "Моите складове", description = "Всички складове на текущия собственик, с назначените агенти.")
    public List<WarehouseDto> warehouses(Principal principal) {
        return warehouseService.findByOwner(principal.getName()).stream()
                .map(WarehouseDto::withAgents)
                .toList();
    }

    @GetMapping("/warehouses/{id}")
    @Operation(summary = "Един мой склад")
    @ApiResponse(responseCode = "200", description = "Складът")
    @ApiResponse(responseCode = "400", description = "Няма такъв склад",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Складът е на друг собственик", content = @Content)
    public WarehouseDto warehouse(@Parameter(description = "id на склада", example = "1") @PathVariable Long id,
                                  Principal principal) {
        return WarehouseDto.withAgents(warehouseService.getOwnedWarehouse(id, principal.getName()));
    }

    @GetMapping("/rentals")
    @Operation(summary = "Заявки и договори за моите складове",
            description = "Всички заявки за наем за складовете на собственика (чакащи, активни, отказани, приключили), най-новите първо.")
    public List<RentalContractDto> rentals(Principal principal) {
        return rentalQueries.contractsForOwner(principal.getName()).stream()
                .map(RentalContractDto::from)
                .toList();
    }

    @GetMapping("/reports/warehouses")
    @Operation(summary = "Справка: моите складове за период (№14)",
            description = "За всеки склад: договорите в периода, наетите дни, заетостта в % и сумата. "
                    + "Броят се активни и приключили договори. По подразбиране периодът е текущият месец.")
    @ApiResponse(responseCode = "200", description = "Редовете на справката")
    @ApiResponse(responseCode = "400", description = "Невалиден период",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public List<OwnerReportRowDto> ownerReport(
            @Parameter(description = "Начална дата", example = "2026-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Крайна дата", example = "2026-11-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        LocalDate today = LocalDate.now();
        LocalDate periodFrom = from != null ? from : today.withDayOfMonth(1);
        LocalDate periodTo = to != null ? to : today.withDayOfMonth(today.lengthOfMonth());
        return ownerReport.build(principal.getName(), periodFrom, periodTo).stream()
                .map(OwnerReportRowDto::from)
                .toList();
    }
}
