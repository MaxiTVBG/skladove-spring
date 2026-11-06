
package bg.skladove.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AgentRatingRepository
        extends JpaRepository<AgentRating, Long> {

    List<AgentRating> findByAgentId(Long agentId);

    boolean existsByRentalContractId(Long rentalContractId);
}
