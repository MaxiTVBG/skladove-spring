package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Справка „Налични складове за отдаване под наем“ за произволен период (задача №13).
 *
 * Кой какво вижда (по заданието):
 *  - складов агент: складовете, към които е назначен (само тях може да отдава);
 *  - собственик: само своите складове.
 *
 * Склад е наличен за периода, ако не е в ремонт / неактивен и няма активен договор,
 * който се застъпва с периода. Тази проверка вече я има в
 * WarehouseAvailabilityService.isAvailable, затова я ползваме, а не я пишем отново.
 */
@Service
public class AvailableWarehouseReportService {

    private static final Logger log = LogManager.getLogger(AvailableWarehouseReportService.class);

    private final WarehouseRepository warehouses;
    private final RentalContractRepository contracts;
    private final AppUserRepository users;
    private final WarehouseAvailabilityService availability;

    public AvailableWarehouseReportService(WarehouseRepository warehouses,
                                           RentalContractRepository contracts,
                                           AppUserRepository users,
                                           WarehouseAvailabilityService availability) {
        this.warehouses = warehouses;
        this.contracts = contracts;
        this.users = users;
        this.availability = availability;
    }

    /**
     * @param category по желание: A, B, C (празно = всички)
     * @param search   по желание: текст в името, адреса, вида стока или климата
     */
    @Transactional(readOnly = true)
    public List<AvailableWarehouse> findAvailable(String username, LocalDate from, LocalDate to,
                                                  String category, String search) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Крайната дата трябва да е след началната.");
        }

        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен."));

        List<Warehouse> candidates = switch (user.getRole()) {
            case AGENT -> warehouses.findByAgentsContaining(user);
            case OWNER -> warehouses.findByOwner(user);
            default -> throw new AccessDeniedException("Справката е за собственици и складови агенти.");
        };

        List<AvailableWarehouse> result = new ArrayList<>();
        for (Warehouse warehouse : candidates) {
            if (!matchesCategory(warehouse, category) || !matchesSearch(warehouse, search)) {
                continue;
            }
            if (!availability.isAvailable(warehouse, from, to)) {
                continue;
            }
            result.add(withFreeDates(warehouse, from, to));
        }
        result.sort(Comparator.comparing(
                (AvailableWarehouse row) -> row.getWarehouse().getMonthlyPrice(),
                Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("Справка 'налични складове': '{}' за {} - {}, намерени {}",
                username, from, to, result.size());
        return result;
    }

    /**
     * Пресмята в какви граници е свободен складът около периода:
     * последният активен договор, който свършва преди периода, и първият, който започва след него.
     */
    private AvailableWarehouse withFreeDates(Warehouse warehouse, LocalDate from, LocalDate to) {
        LocalDate freeSince = null;
        LocalDate freeUntil = null;
        for (RentalContract contract : contracts.findByWarehouseIdAndStatus(warehouse.getId(), RentalStatus.ACTIVE)) {
            if (contract.getEndDate().isBefore(from)) {
                LocalDate dayAfter = contract.getEndDate().plusDays(1);
                if (freeSince == null || dayAfter.isAfter(freeSince)) {
                    freeSince = dayAfter;
                }
            } else if (contract.getStartDate().isAfter(to)) {
                LocalDate dayBefore = contract.getStartDate().minusDays(1);
                if (freeUntil == null || dayBefore.isBefore(freeUntil)) {
                    freeUntil = dayBefore;
                }
            }
        }
        return new AvailableWarehouse(warehouse, freeSince, freeUntil);
    }

    private static boolean matchesCategory(Warehouse w, String category) {
        return category == null || category.isBlank() || category.equals(w.getCategory());
    }

    private static boolean matchesSearch(Warehouse w, String search) {
        if (search == null || search.isBlank()) {
            return true;
        }
        String needle = search.trim().toLowerCase(Locale.ROOT);
        return contains(w.getName(), needle) || contains(w.getAddress(), needle)
                || contains(w.getGoodsType(), needle) || contains(w.getClimateConditions(), needle);
    }

    private static boolean contains(String text, String needle) {
        return text != null && text.toLowerCase(Locale.ROOT).contains(needle);
    }
}
