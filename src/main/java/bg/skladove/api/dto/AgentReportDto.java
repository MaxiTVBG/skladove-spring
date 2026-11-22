package bg.skladove.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "Справка за складов агент за период")
public record AgentReportDto(
        @Schema(example = "petar") String agentUsername,
        @Schema(description = "Среден рейтинг (1–5); null, ако още няма оценки", example = "4.5") Double rating,
        LocalDate from,
        LocalDate to,
        @Schema(description = "Приключилите договори на агента, създадени в периода")
        List<RentalContractDto> completedContracts) {
}
