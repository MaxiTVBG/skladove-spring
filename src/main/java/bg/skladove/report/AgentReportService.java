
package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class AgentReportService {

    private final RentalContractRepository contractRepository;
    private final AppUserRepository userRepository;

    public AgentReportService(
            RentalContractRepository contractRepository,
            AppUserRepository userRepository) {
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<RentalContract> getCompletedContracts(
            String agentUsername,
            LocalDate from,
            LocalDate to) {

        if (from == null || to == null || from.isAfter(to)) {
            throw new IllegalArgumentException(
                    "Невалиден период за справката.");
        }

        AppUser agent = userRepository.findByUsername(agentUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Складовият агент не е намерен."));

        if (agent.getRole() != bg.skladove.user.Role.AGENT) {
            throw new IllegalArgumentException(
                    "Само складови агенти могат да преглеждат тази справка.");
        }

        return contractRepository
                .findWithDetailsByAgentOrderByCreatedAtDesc(agent)
                .stream()
                .filter(contract ->
                        contract.getStatus() == RentalStatus.COMPLETED)
                .filter(contract ->
                        !contract.getCreatedAt().isBefore(from)
                                && !contract.getCreatedAt().isAfter(to))
                .toList();
    }

    @Transactional(readOnly = true)
    public Double getAgentRating(String agentUsername) {
        AppUser agent = userRepository.findByUsername(agentUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Складовият агент не е намерен."));

        if (agent.getRole() != bg.skladove.user.Role.AGENT) {
            throw new IllegalArgumentException(
                    "Само складови агенти могат да преглеждат тази справка.");
        }

        return agent.getRating();
    }

}
