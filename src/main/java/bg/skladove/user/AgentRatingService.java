
package bg.skladove.user;
import java.util.stream.Collectors;
import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AgentRatingService {

    private final AgentRatingRepository ratingRepository;
    private final RentalContractRepository contractRepository;
    private final AppUserRepository userRepository;

    public AgentRatingService(
            AgentRatingRepository ratingRepository,
            RentalContractRepository contractRepository,
            AppUserRepository userRepository) {

        this.ratingRepository = ratingRepository;
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AgentRating submitRating(
            Long contractId,
            String ownerUsername,
            Integer score,
            String comment) {

        if (score == null || score < 1 || score > 5) {
            throw new IllegalArgumentException(
                    "Оценката трябва да бъде между 1 и 5.");
        }

        RentalContract contract = contractRepository.findById(contractId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Наемът не е намерен."));

        AppUser owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException("Собственикът не е намерен."));

        if (owner.getRole() != Role.OWNER) {
            throw new IllegalArgumentException(
                    "Само собственици могат да оценяват агенти.");
        }

        if (!contract.getWarehouse().getOwner().getId().equals(owner.getId())) {
            throw new IllegalArgumentException(
                    "Нямате право да оценявате този наем.");
        }

        if (contract.getStatus() != RentalStatus.COMPLETED) {
            throw new IllegalArgumentException(
                    "Може да оценявате само приключени наеми.");
        }

        if (ratingRepository.existsByRentalContractId(contractId)) {
            throw new IllegalArgumentException(
                    "Този наем вече е оценен.");
        }

        AppUser agent = contract.getAgent();

        AgentRating rating = new AgentRating();
        rating.setAgent(agent);
        rating.setOwner(owner);
        rating.setRentalContract(contract);
        rating.setScore(score);
        rating.setComment(comment == null || comment.isBlank()
                ? null : comment.trim());

        AgentRating savedRating = ratingRepository.save(rating);

        List<AgentRating> ratings =
                ratingRepository.findByAgentId(agent.getId());

        double average = ratings.stream()
                .mapToInt(AgentRating::getScore)
                .average()
                .orElse(0.0);

        agent.setRating(average);
        userRepository.save(agent);

        return savedRating;
    }
    @Transactional(readOnly = true)
    public List<RentalContract> findRateableContracts(String ownerUsername) {

        AppUser owner = userRepository.findByUsername(ownerUsername)
                .orElseThrow(() ->
                        new IllegalArgumentException("Собственикът не е намерен."));

        List<RentalContract> completedContracts =
                contractRepository.findWithDetailsByWarehouse_Owner_IdOrderByCreatedAtDesc(
                        owner.getId()
                );

        return completedContracts.stream()
                .filter(contract ->
                        contract.getStatus() == RentalStatus.COMPLETED)
                .filter(contract ->
                        !ratingRepository.existsByRentalContractId(contract.getId()))
                .toList();
    }
}
