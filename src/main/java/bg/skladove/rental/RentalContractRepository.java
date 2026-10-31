package bg.skladove.rental;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RentalContractRepository
        extends JpaRepository<RentalContract, Long> {

    // Agent report: contracts created during a period
    List<RentalContract> findByAgentAndCreatedAtBetween(
            AppUser agent,
            LocalDate from,
            LocalDate to
    );

    // Notifications: contracts ending during a period
    List<RentalContract> findByEndDateBetween(
            LocalDate from,
            LocalDate to
    );

    // All contracts belonging to an agent
    List<RentalContract> findByAgent(AppUser agent);

    // All contracts for a particular warehouse
    List<RentalContract> findByWarehouseId(Long warehouseId);

    // Contracts with a particular status
    List<RentalContract> findByStatus(RentalStatus status);
    // Проверява договорите за конкретен склад и статус
    List<RentalContract> findByWarehouseIdAndStatus(
            Long warehouseId,
            RentalStatus status
    );

    // Намира активни договори, чиято крайна дата е преди днешната
    List<RentalContract> findByStatusAndEndDateBefore(
            RentalStatus status,
            LocalDate date
    );
    // Pending requests for a specific owner
    List<RentalContract>
    findByWarehouse_Owner_IdAndStatus(
            Long ownerId,
            RentalStatus status
    );
}