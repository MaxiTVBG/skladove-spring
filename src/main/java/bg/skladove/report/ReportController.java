package bg.skladove.report;

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
 * Страници със справки.
 *
 * Справките са GET заявки с параметри в адреса (?from=...&to=...),
 * така че резултатът може да се презареди или да се запази като линк.
 */
@Controller
public class ReportController {

    static final List<String> CATEGORIES = List.of("A", "B", "C");

    private final AvailableWarehouseReportService availableReport;
    private final OwnerWarehouseReportService ownerReport;

    public ReportController(AvailableWarehouseReportService availableReport,
                            OwnerWarehouseReportService ownerReport) {
        this.availableReport = availableReport;
        this.ownerReport = ownerReport;
    }

    /**
     * Налични складове за период (задача №13). За собственици и агенти;
     * кой какво вижда решава AvailableWarehouseReportService.
     * По подразбиране периодът е от днес до след един месец.
     */
    @GetMapping("/reports/available")
    public String available(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                            @RequestParam(required = false) String category,
                            @RequestParam(required = false) String search,
                            Model model,
                            Principal principal) {
        LocalDate periodFrom = from != null ? from : LocalDate.now();
        LocalDate periodTo = to != null ? to : periodFrom.plusMonths(1);

        model.addAttribute("from", periodFrom);
        model.addAttribute("to", periodTo);
        model.addAttribute("category", category);
        model.addAttribute("search", search);
        model.addAttribute("categories", CATEGORIES);

        try {
            model.addAttribute("rows", availableReport.findAvailable(
                    principal.getName(), periodFrom, periodTo, category, search));
        } catch (IllegalArgumentException e) {
            model.addAttribute("rows", List.of());
            model.addAttribute("errorMessage", e.getMessage());
        }
        return "reports/available";
    }

    /**
     * Складовете на собственика за период (задача №14).
     * Адресът е под /owner/**, затова е достъпен само за роля OWNER.
     * По подразбиране периодът е текущият месец.
     */
    @GetMapping("/owner/reports/warehouses")
    public String ownerWarehouses(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
                                  @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
                                  Model model,
                                  Principal principal) {
        LocalDate today = LocalDate.now();
        LocalDate periodFrom = from != null ? from : today.withDayOfMonth(1);
        LocalDate periodTo = to != null ? to : today.withDayOfMonth(today.lengthOfMonth());

        model.addAttribute("from", periodFrom);
        model.addAttribute("to", periodTo);

        List<OwnerWarehouseReportRow> rows;
        try {
            rows = ownerReport.build(principal.getName(), periodFrom, periodTo);
        } catch (IllegalArgumentException e) {
            rows = List.of();
            model.addAttribute("errorMessage", e.getMessage());
        }
        model.addAttribute("rows", rows);

        // Обобщение най-горе на страницата
        model.addAttribute("totalContracts", rows.stream().mapToInt(r -> r.getContracts().size()).sum());
        model.addAttribute("totalAmount", rows.stream()
                .map(OwnerWarehouseReportRow::getContractsTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        model.addAttribute("averageOccupancy", (int) Math.round(rows.stream()
                .mapToInt(OwnerWarehouseReportRow::getOccupancyPercent)
                .average().orElse(0)));
        return "owner/reports/warehouses";
    }
}
