package bg.skladove.ai;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST endpoint за препоръка на цена. Извиква се с JavaScript от формата за склад
 * (бутон „Предложи цена“) и връща JSON, без да презарежда страницата.
 *
 * Адресът е под /owner/**, затова е достъпен само за роля OWNER (SecurityConfig).
 */
@RestController
@RequestMapping("/owner/api/ai")
@Tag(name = "AI: препоръка за цена", description = "Модел k-NN за месечна цена на склад (роля OWNER)")
public class PriceRecommendationController {

    private final PriceRecommendationService service;

    public PriceRecommendationController(PriceRecommendationService service) {
        this.service = service;
    }

    /**
     * Пример: GET /owner/api/ai/price-recommendation?area=120&height=6&category=B&climate=Хладилен
     *
     * @param warehouseId при редакция — id на склада, за да не участва в собствената си оценка
     */
    @GetMapping("/price-recommendation")
    @Operation(summary = "Препоръчана месечна цена за склад",
            description = "Намира 3-те най-подобни склада (по площ, височина, категория и климат) "
                    + "и връща среднопретеглената им цена на м², умножена по площта, заедно с диапазон и самите съседи.")
    @ApiResponse(responseCode = "200", description = "Препоръката",
            content = @Content(schema = @Schema(implementation = PriceRecommendation.class)))
    @ApiResponse(responseCode = "400", description = "Липсва площ или няма достатъчно данни",
            content = @Content(schema = @Schema(example = "{\"error\": \"Въведете площ, за да се изчисли цена.\"}")))
    public ResponseEntity<?> recommend(
            @Parameter(description = "Площ в м²", example = "120", required = true)
            @RequestParam(required = false) Double area,
            @Parameter(description = "Височина в м", example = "6")
            @RequestParam(required = false) Double height,
            @Parameter(description = "Категория: A, B или C", example = "B")
            @RequestParam(required = false) String category,
            @Parameter(description = "Климат (свободен текст, напр. Сух, Хладилен)", example = "Хладилен")
            @RequestParam(required = false) String climate,
            @Parameter(description = "При редакция: id на склада, за да не участва в собствената си оценка")
            @RequestParam(required = false) Long warehouseId) {
        PriceFeatures query = new PriceFeatures(area, height,
                category == null || category.isBlank() ? null : category,
                ClimateClass.fromText(climate));
        try {
            return ResponseEntity.ok(service.recommend(query, warehouseId));
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/price-model/evaluation")
    @Operation(summary = "Проверка на модела за цена (leave-one-out)",
            description = "За k от 1 до 10: средна грешка на модела в лева и проценти "
                    + "и същото за базовия метод (медианна цена на м² × площ). Изчислява се върху текущите данни.")
    public List<ModelEvaluation> evaluation() {
        return service.evaluate();
    }
}
