package bg.skladove.ai;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Препоръка на склад за клиент (задача №15).
 *
 * 1. Взимат се складовете, към които агентът е назначен и които са свободни за периода.
 * 2. Всеки склад се оценява спрямо изискванията на клиента ({@link WarehouseScorer}).
 * 3. Складове с неподходящ климат (стоката би се развалила) отпадат.
 * 4. За всеки склад моделът за цена ({@link KnnPriceModel}) дава пазарна оценка,
 *    за да види агентът дали обявената цена е изгодна.
 * 5. Резултатът е подреден по процент съвпадение.
 */
@Service
public class WarehouseRecommendationService {

    private static final Logger log = LogManager.getLogger(WarehouseRecommendationService.class);

    private final WarehouseRepository warehouses;
    private final AppUserRepository users;
    private final WarehouseAvailabilityService availability;
    private final PriceRecommendationService prices;

    public WarehouseRecommendationService(WarehouseRepository warehouses,
                                          AppUserRepository users,
                                          WarehouseAvailabilityService availability,
                                          PriceRecommendationService prices) {
        this.warehouses = warehouses;
        this.users = users;
        this.availability = availability;
        this.prices = prices;
    }

    @Transactional(readOnly = true)
    public List<WarehouseMatch> recommend(String agentUsername, WarehouseRequest request) {
        if (request.area() == null || request.area() <= 0) {
            throw new IllegalArgumentException("Въведете нужната площ.");
        }
        if (request.from() == null || request.to() == null || request.to().isBefore(request.from())) {
            throw new IllegalArgumentException("Крайната дата трябва да е след началната.");
        }

        AppUser agent = users.findByUsername(agentUsername)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен."));
        if (agent.getRole() != Role.AGENT) {
            throw new AccessDeniedException("Препоръката на склад е за складови агенти.");
        }

        List<Warehouse> candidates = warehouses.findByAgentsContaining(agent).stream()
                .filter(w -> availability.isAvailable(w, request.from(), request.to()))
                .toList();
        Map<Long, BigDecimal> estimates = prices.estimateMarketPrices(candidates);

        List<WarehouseMatch> result = new ArrayList<>();
        for (Warehouse w : candidates) {
            WarehouseScorer.Score score = WarehouseScorer.score(request, w);
            if (!score.suitable()) {
                continue;
            }
            BigDecimal estimate = estimates.get(w.getId());
            result.add(new WarehouseMatch(w, score.percent(), score.pros(), score.cons(),
                    estimate, percentDifference(w.getMonthlyPrice(), estimate)));
        }
        result.sort(Comparator.comparingInt(WarehouseMatch::matchPercent).reversed()
                .thenComparing(m -> m.warehouse().getMonthlyPrice(),
                        Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("AI препоръка на склад: агент '{}', площ {} м², климат {}, бюджет {} -> {} от {} свободни склада",
                agentUsername, request.area(), request.climate(), request.budget(), result.size(), candidates.size());
        return result;
    }

    /** С колко % цената е над (+) или под (−) оценката. */
    static Integer percentDifference(BigDecimal price, BigDecimal estimate) {
        if (price == null || estimate == null || estimate.signum() <= 0) {
            return null;
        }
        return price.subtract(estimate)
                .multiply(BigDecimal.valueOf(100))
                .divide(estimate, 0, RoundingMode.HALF_UP)
                .intValue();
    }
}
