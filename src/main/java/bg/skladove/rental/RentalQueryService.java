package bg.skladove.rental;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Четене на данни за страниците с наеми: кои складове обслужва агентът,
 * кои заявки и договори вижда агентът и кои собственикът.
 *
 * Записът (заявка, одобрение, отхвърляне) е в RentalService.
 * Разделени са, за да е ясно кое променя данни и кое само ги показва.
 */
@Service
public class RentalQueryService {

    private final RentalContractRepository contracts;
    private final WarehouseRepository warehouses;
    private final AppUserRepository users;

    public RentalQueryService(RentalContractRepository contracts,
                              WarehouseRepository warehouses,
                              AppUserRepository users) {
        this.contracts = contracts;
        this.warehouses = warehouses;
        this.users = users;
    }

    /** Влезлият потребител, като се проверява, че има очакваната роля. */
    @Transactional(readOnly = true)
    public AppUser getUser(String username, Role expectedRole) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен."));
        if (user.getRole() != expectedRole) {
            throw new AccessDeniedException("Нямате достъп до тази страница.");
        }
        return user;
    }

    /** Складовете, към които агентът е назначен (само за тях може да прави заявки). */
    @Transactional(readOnly = true)
    public List<Warehouse> warehousesForAgent(String agentUsername) {
        return warehouses.findByAgentsContaining(getUser(agentUsername, Role.AGENT));
    }

    /** Един склад, но само ако агентът е назначен към него. */
    @Transactional(readOnly = true)
    public Warehouse warehouseForAgent(Long warehouseId, String agentUsername) {
        AppUser agent = getUser(agentUsername, Role.AGENT);
        Warehouse warehouse = warehouses.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Складът не е намерен."));
        boolean assigned = warehouse.getAgents().stream()
                .anyMatch(a -> a.getId().equals(agent.getId()));
        if (!assigned) {
            throw new AccessDeniedException("Не сте назначен като агент на този склад.");
        }
        return warehouse;
    }

    /** Заявките и договорите на агента, най-новите първи. */
    @Transactional(readOnly = true)
    public List<RentalContract> contractsForAgent(String agentUsername) {
        return contracts.findWithDetailsByAgentOrderByCreatedAtDesc(getUser(agentUsername, Role.AGENT));
    }

    /** Всички заявки и договори за складовете на собственика, най-новите първи. */
    @Transactional(readOnly = true)
    public List<RentalContract> contractsForOwner(String ownerUsername) {
        AppUser owner = getUser(ownerUsername, Role.OWNER);
        return contracts.findWithDetailsByWarehouse_Owner_IdOrderByCreatedAtDesc(owner.getId());
    }
}
