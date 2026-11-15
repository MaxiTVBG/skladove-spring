package bg.skladove.rental;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
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

    // За страниците с наеми: зарежда склада, агента и собственика, който е решил,
    // с една заявка. Те са LAZY, а open-in-view е изключено.
    @EntityGraph(attributePaths = {"warehouse", "agent", "decidedBy"})
    List<RentalContract> findWithDetailsByAgentOrderByCreatedAtDesc(AppUser agent);

    @EntityGraph(attributePaths = {"warehouse", "agent", "decidedBy"})
    List<RentalContract> findWithDetailsByWarehouse_Owner_IdOrderByCreatedAtDesc(Long ownerId);

    // Справка „Складове на собственик“ (задача №14): договорите за складовете на собственика,
    // които се застъпват с периода [from, to]. Застъпване значи: започва до края на периода
    // и свършва след началото му. JOIN FETCH зарежда склада и агента в същата заявка.
    @Query("""
            select c from RentalContract c
            join fetch c.warehouse w
            join fetch c.agent
            where w.owner.id = :ownerId
              and c.status in :statuses
              and c.startDate <= :to
              and c.endDate >= :from
            order by c.startDate
            """)
    List<RentalContract> findOwnerContractsInPeriod(@Param("ownerId") Long ownerId,
                                                    @Param("statuses") Collection<RentalStatus> statuses,
                                                    @Param("from") LocalDate from,
                                                    @Param("to") LocalDate to);
}
