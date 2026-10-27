package bg.skladove.rental;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RentalContractRepository extends JpaRepository<RentalContract, Long> {

    /** Справка: договори на складов агент за период. */
    List<RentalContract> findByAgentAndCreatedAtBetween(AppUser agent, LocalDate from, LocalDate to);

    /** За известия за изтичащи договори. */
    List<RentalContract> findByEndDateBetween(LocalDate from, LocalDate to);
}
