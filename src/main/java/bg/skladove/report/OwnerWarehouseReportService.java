package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Справка „Складове на собственик“ за произволен период (задача №14).
 *
 * По заданието собственикът вижда справка само за своите складове, но за всички тях.
 * Затова тръгваме от всичките му складове (и тези без договори),
 * а договорите им в периода ги взимаме с една заявка към базата.
 *
 * В справката влизат само договори, които реално са сключени: активни и приключили.
 * Чакащите и отхвърлените заявки не са наем.
 */
@Service
public class OwnerWarehouseReportService {

    private static final Logger log = LogManager.getLogger(OwnerWarehouseReportService.class);

    static final List<RentalStatus> COUNTED_STATUSES = List.of(RentalStatus.ACTIVE, RentalStatus.COMPLETED);

    private final WarehouseRepository warehouses;
    private final RentalContractRepository contracts;
    private final AppUserRepository users;

    public OwnerWarehouseReportService(WarehouseRepository warehouses,
                                       RentalContractRepository contracts,
                                       AppUserRepository users) {
        this.warehouses = warehouses;
        this.contracts = contracts;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public List<OwnerWarehouseReportRow> build(String ownerUsername, LocalDate from, LocalDate to) {
        if (from == null || to == null || to.isBefore(from)) {
            throw new IllegalArgumentException("Крайната дата трябва да е след началната.");
        }

        AppUser owner = users.findByUsername(ownerUsername)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен."));
        if (owner.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Справката е само за собственици.");
        }

        // Договорите в периода, групирани по склад (id на склада -> договори)
        Map<Long, List<RentalContract>> contractsByWarehouse = contracts
                .findOwnerContractsInPeriod(owner.getId(), COUNTED_STATUSES, from, to)
                .stream()
                .collect(Collectors.groupingBy(c -> c.getWarehouse().getId()));

        long periodDays = ChronoUnit.DAYS.between(from, to) + 1; // двете дати се броят

        List<OwnerWarehouseReportRow> rows = new ArrayList<>();
        for (Warehouse warehouse : warehouses.findByOwner(owner)) {
            List<RentalContract> inPeriod = contractsByWarehouse.getOrDefault(warehouse.getId(), List.of());
            rows.add(new OwnerWarehouseReportRow(
                    warehouse,
                    inPeriod,
                    rentedDays(inPeriod, from, to),
                    periodDays,
                    total(inPeriod)));
        }
        rows.sort(Comparator.comparing((OwnerWarehouseReportRow r) -> r.getWarehouse().getName(),
                Comparator.nullsLast(Comparator.naturalOrder())));

        log.info("Справка 'складове на собственик': '{}' за {} - {}, складове {}",
                ownerUsername, from, to, rows.size());
        return rows;
    }

    /**
     * Колко дни от периода складът е бил нает.
     * Всеки договор се „отрязва“ до границите на периода; ако два договора
     * се застъпват, общите дни се броят веднъж (интервалите се сливат).
     */
    static long rentedDays(List<RentalContract> contracts, LocalDate from, LocalDate to) {
        List<LocalDate[]> intervals = new ArrayList<>();
        for (RentalContract c : contracts) {
            LocalDate start = c.getStartDate().isBefore(from) ? from : c.getStartDate();
            LocalDate end = c.getEndDate().isAfter(to) ? to : c.getEndDate();
            if (!end.isBefore(start)) {
                intervals.add(new LocalDate[]{start, end});
            }
        }
        intervals.sort(Comparator.comparing(i -> i[0]));

        long days = 0;
        LocalDate currentStart = null;
        LocalDate currentEnd = null;
        for (LocalDate[] interval : intervals) {
            if (currentEnd != null && !interval[0].isAfter(currentEnd.plusDays(1))) {
                // застъпва се или е съседен на текущия: разширяваме
                if (interval[1].isAfter(currentEnd)) {
                    currentEnd = interval[1];
                }
            } else {
                if (currentEnd != null) {
                    days += ChronoUnit.DAYS.between(currentStart, currentEnd) + 1;
                }
                currentStart = interval[0];
                currentEnd = interval[1];
            }
        }
        if (currentEnd != null) {
            days += ChronoUnit.DAYS.between(currentStart, currentEnd) + 1;
        }
        return days;
    }

    private static BigDecimal total(List<RentalContract> contracts) {
        return contracts.stream()
                .map(RentalContract::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
