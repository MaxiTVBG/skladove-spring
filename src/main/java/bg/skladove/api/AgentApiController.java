package bg.skladove.api;

import bg.skladove.ai.ClimateClass;
import bg.skladove.ai.WarehouseRecommendationService;
import bg.skladove.ai.WarehouseRequest;
import bg.skladove.api.dto.AgentReportDto;
import bg.skladove.api.dto.ApiError;
import bg.skladove.api.dto.RentalContractDto;
import bg.skladove.api.dto.WarehouseDto;
import bg.skladove.api.dto.WarehouseMatchDto;
import bg.skladove.rental.RentalQueryService;
import bg.skladove.report.AgentReportService;
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

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * REST API за складовия агент (само четене). Адресите са под /agent/**, затова са достъпни само за роля AGENT.
 */
@RestController
@RequestMapping("/agent/api")
@Tag(name = "Складов агент", description = "Складове, договори, справка и AI препоръка на склад (роля AGENT)")
public class AgentApiController {

    private final RentalQueryService rentalQueries;
    private final AgentReportService agentReport;
    private final WarehouseRecommendationService recommendations;

    public AgentApiController(RentalQueryService rentalQueries,
                              AgentReportService agentReport,
                              WarehouseRecommendationService recommendations) {
        this.rentalQueries = rentalQueries;
        this.agentReport = agentReport;
        this.recommendations = recommendations;
    }

    @GetMapping("/warehouses")
    @Operation(summary = "Складовете, към които съм назначен")
    public List<WarehouseDto> warehouses(Principal principal) {
        return rentalQueries.warehousesForAgent(principal.getName()).stream()
                .map(WarehouseDto::from)
                .toList();
    }

    @GetMapping("/rentals")
    @Operation(summary = "Моите заявки и договори", description = "Всички заявки за наем, подадени от агента, най-новите първо.")
    public List<RentalContractDto> rentals(Principal principal) {
        return rentalQueries.contractsForAgent(principal.getName()).stream()
                .map(RentalContractDto::from)
                .toList();
    }

    @GetMapping("/report")
    @Operation(summary = "Справка: моите приключили договори и рейтинг за период",
            description = "По подразбиране периодът е от началото на текущия месец до днес.")
    @ApiResponse(responseCode = "200", description = "Справката")
    @ApiResponse(responseCode = "400", description = "Невалиден период",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public AgentReportDto report(
            @Parameter(example = "2026-11-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(example = "2026-11-30")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        LocalDate periodFrom = from != null ? from : LocalDate.now().withDayOfMonth(1);
        LocalDate periodTo = to != null ? to : LocalDate.now();
        String username = principal.getName();
        return new AgentReportDto(username,
                agentReport.getAgentRating(username),
                periodFrom, periodTo,
                agentReport.getCompletedContracts(username, periodFrom, periodTo).stream()
                        .map(RentalContractDto::from)
                        .toList());
    }

    @GetMapping("/ai/recommend")
    @Operation(summary = "AI препоръка на склад по изискванията на клиента",
            description = "Подрежда свободните за периода складове на агента по процент съвпадение "
                    + "(площ 30%, климат 30%, бюджет 25%, вид стока 10%, категория 5%). "
                    + "Складове с неподходящ климат отпадат. Към всеки склад има AI оценка на пазарната цена.")
    @ApiResponse(responseCode = "200", description = "Препоръчаните складове, най-подходящите първо")
    @ApiResponse(responseCode = "400", description = "Липсва площ или периодът е невалиден",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public List<WarehouseMatchDto> recommend(
            @Parameter(description = "Нужна площ в м²", example = "100", required = true)
            @RequestParam(required = false) Double area,
            @Parameter(description = "Климат за стоката: Сух, Отопляем, Хладилен или Фризер", example = "Хладилен")
            @RequestParam(required = false) String climate,
            @Parameter(description = "Вид стока", example = "хранителни продукти")
            @RequestParam(required = false) String goodsType,
            @Parameter(description = "Категория: A, B или C", example = "B")
            @RequestParam(required = false) String category,
            @Parameter(description = "Бюджет на месец в лв.", example = "2000")
            @RequestParam(required = false) BigDecimal budget,
            @Parameter(description = "Начало на периода (по подразбиране днес)", example = "2026-12-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Край на периода (по подразбиране след месец)", example = "2026-12-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Principal principal) {
        LocalDate periodFrom = from != null ? from : LocalDate.now();
        LocalDate periodTo = to != null ? to : periodFrom.plusMonths(1);
        WarehouseRequest request = new WarehouseRequest(area, ClimateClass.fromText(climate), goodsType,
                category == null || category.isBlank() ? null : category, budget, periodFrom, periodTo);
        return recommendations.recommend(principal.getName(), request).stream()
                .map(WarehouseMatchDto::from)
                .toList();
    }
}
