package bg.skladove.api.dto;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Договор (заявка) за наем")
public record RentalContractDto(
        @Schema(example = "7") Long id,
        @Schema(example = "1") Long warehouseId,
        @Schema(example = "Склад Север") String warehouseName,
        @Schema(example = "petar") String agentUsername,
        @Schema(example = "Петър Петров") String agentName,
        @Schema(example = "Иван Иванов") String tenantName,
        @Schema(example = "0888123456") String tenantPhone,
        @Schema(example = "ivan@example.com") String tenantEmail,
        LocalDate startDate,
        LocalDate endDate,
        @Schema(description = "Цена за целия период в лв.", example = "3600.00") BigDecimal price,
        RentalStatus status,
        LocalDate createdAt,
        @Schema(description = "Бележка на собственика при отказ") String decisionNote) {

    /** Изисква warehouse и agent да са заредени (repository методите с @EntityGraph / join fetch). */
    public static RentalContractDto from(RentalContract c) {
        return new RentalContractDto(c.getId(),
                c.getWarehouse().getId(), c.getWarehouse().getName(),
                c.getAgent().getUsername(), c.getAgent().getFullName(),
                c.getTenantName(), c.getTenantPhone(), c.getTenantEmail(),
                c.getStartDate(), c.getEndDate(), c.getPrice(), c.getStatus(),
                c.getCreatedAt(), c.getDecisionNote());
    }
}
