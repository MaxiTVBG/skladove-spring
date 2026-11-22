package bg.skladove.ai;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Страница „Как работи препоръката за цена“: данни, метод и резултати от проверката на модела.
 */
@Controller
public class PriceModelController {

    private final PriceRecommendationService service;

    public PriceModelController(PriceRecommendationService service) {
        this.service = service;
    }

    @GetMapping("/owner/ai/price-model")
    public String priceModel(Model model) {
        List<PriceSample> data = service.trainingData(null);
        long systemCount = data.stream().filter(s -> s.source() == PriceSample.Source.SYSTEM).count();

        model.addAttribute("evaluations", service.evaluate());
        model.addAttribute("chosenK", KnnPriceModel.DEFAULT_K);
        model.addAttribute("sampleCount", data.size());
        model.addAttribute("systemCount", systemCount);
        model.addAttribute("referenceCount", data.size() - systemCount);
        model.addAttribute("weightArea", KnnPriceModel.WEIGHT_AREA);
        model.addAttribute("weightHeight", KnnPriceModel.WEIGHT_HEIGHT);
        model.addAttribute("weightCategory", KnnPriceModel.WEIGHT_CATEGORY);
        model.addAttribute("weightClimate", KnnPriceModel.WEIGHT_CLIMATE);
        return "owner/ai/price-model";
    }
}
