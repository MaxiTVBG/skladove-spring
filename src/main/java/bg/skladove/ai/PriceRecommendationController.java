package bg.skladove.ai;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST endpoint за препоръка на цена. Извиква се с JavaScript от формата за склад
 * (бутон „Предложи цена“) и връща JSON, без да презарежда страницата.
 *
 * Адресът е под /owner/**, затова е достъпен само за роля OWNER (SecurityConfig).
 */
@RestController
@RequestMapping("/owner/api/ai")
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
    public ResponseEntity<?> recommend(@RequestParam(required = false) Double area,
                                       @RequestParam(required = false) Double height,
                                       @RequestParam(required = false) String category,
                                       @RequestParam(required = false) String climate,
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
}
