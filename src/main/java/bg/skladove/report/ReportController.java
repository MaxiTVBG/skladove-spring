package bg.skladove.report;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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

    public ReportController(AvailableWarehouseReportService availableReport) {
        this.availableReport = availableReport;
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
}
