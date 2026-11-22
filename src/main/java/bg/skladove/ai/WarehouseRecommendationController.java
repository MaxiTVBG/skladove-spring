package bg.skladove.ai;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;

/**
 * Страница „Препоръка на склад“ за складовия агент.
 * Адресът е под /agent/**, затова е достъпен само за роля AGENT.
 *
 * Формата е GET: изискванията остават в адреса и резултатът може да се презареди.
 */
@Controller
public class WarehouseRecommendationController {

    static final List<String> CATEGORIES = List.of("A", "B", "C");

    private final WarehouseRecommendationService service;

    public WarehouseRecommendationController(WarehouseRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/agent/ai/recommend")
    public String recommend(@RequestParam(required = false) Double area,
                            @RequestParam(required = false) String climate,
                            @RequestParam(required = false) String goodsType,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) BigDecimal budget,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            Model model,
                            Principal principal) {
        LocalDate periodFrom = from != null ? from : LocalDate.now();
        LocalDate periodTo = to != null ? to : periodFrom.plusMonths(1);

        model.addAttribute("area", area);
        model.addAttribute("climate", climate);
        model.addAttribute("goodsType", goodsType);
        model.addAttribute("category", category);
        model.addAttribute("budget", budget);
        model.addAttribute("from", periodFrom);
        model.addAttribute("to", periodTo);
        model.addAttribute("categories", CATEGORIES);
        model.addAttribute("climates", ClimateClass.values());

        // Без площ формата е празна: показваме само полетата
        if (area != null) {
            WarehouseRequest request = new WarehouseRequest(area, ClimateClass.fromText(climate),
                    goodsType, category == null || category.isBlank() ? null : category,
                    budget, periodFrom, periodTo);
            try {
                model.addAttribute("matches", service.recommend(principal.getName(), request));
            } catch (IllegalArgumentException e) {
                model.addAttribute("errorMessage", e.getMessage());
            }
        }
        return "agent/ai/recommend";
    }
}
