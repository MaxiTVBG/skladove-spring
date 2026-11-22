package bg.skladove.api;

import bg.skladove.api.dto.ApiError;
import bg.skladove.api.dto.AvailableWarehouseDto;
import bg.skladove.report.AvailableWarehouseReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Справки, общи за собственици и агенти. Кой какво вижда решава самата услуга:
 * агентът — складовете, към които е назначен; собственикът — своите; администраторът получава 403.
 */
@RestController
@RequestMapping("/api/reports")
@Tag(name = "Справки", description = "Справки за собственици и складови агенти")
public class ReportApiController {

    private final AvailableWarehouseReportService availableReport;

    public ReportApiController(AvailableWarehouseReportService availableReport) {
        this.availableReport = availableReport;
    }

    @GetMapping("/available")
    @Operation(summary = "Справка: налични складове за период (№13)",
            description = "Свободните за периода складове, подредени по цена. "
                    + "По подразбиране периодът е от днес до след един месец.")
    @ApiResponse(responseCode = "200", description = "Свободните складове")
    @ApiResponse(responseCode = "400", description = "Невалиден период",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "403", description = "Потребителят не е собственик или агент", content = @Content)
    public List<AvailableWarehouseDto> available(
            @Parameter(example = "2026-12-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Категория: A, B или C", example = "B")
            @RequestParam(required = false) String category,
            @Parameter(description = "Текст в името, адреса, вида стока или климата", example = "хладилен")
            @RequestParam(required = false) String search,
            Principal principal) {
        LocalDate periodFrom = from != null ? from : LocalDate.now();
        LocalDate periodTo = to != null ? to : periodFrom.plusMonths(1);
        return availableReport.findAvailable(principal.getName(), periodFrom, periodTo, category, search).stream()
                .map(AvailableWarehouseDto::from)
                .toList();
    }
}
