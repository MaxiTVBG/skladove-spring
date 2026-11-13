
package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/agent/report")
public class AgentReportController {

    private final AgentReportService reportService;

    public AgentReportController(AgentReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping
    public String showReport(
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to,

            Authentication authentication,
            Model model) {

        if (from == null) {
            from = LocalDate.now().withDayOfMonth(1);
        }

        if (to == null) {
            to = LocalDate.now();
        }

        List<RentalContract> contracts =
                reportService.getCompletedContracts(
                        authentication.getName(), from, to);

        model.addAttribute("contracts", contracts);
        model.addAttribute("from", from);
        model.addAttribute("to", to);

        model.addAttribute("agentRating",
                reportService.getAgentRating(authentication.getName()));

        return "agent/report";
    }
}
