package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Бизнес логика за складовете на собственика: създаване, редакция, преглед
 * и избор на складови агенти.
 *
 * Основно правило: собственикът вижда и променя само своите складове.
 * Проверката е тук, в service-а, а не само в контролера, за да важи
 * за всеки, който извиква тези методи.
 */
@Service
public class WarehouseService {

    private static final Logger log = LogManager.getLogger(WarehouseService.class);

    private final WarehouseRepository warehouses;
    private final AppUserRepository users;

    // Dependency injection през конструктора: Spring сам подава repository-тата.
    public WarehouseService(WarehouseRepository warehouses, AppUserRepository users) {
        this.warehouses = warehouses;
        this.users = users;
    }

    /** Всички складове на даден собственик, заедно с агентите им. */
    @Transactional(readOnly = true)
    public List<Warehouse> findByOwner(String ownerUsername) {
        return warehouses.findWithAgentsByOwner(getOwner(ownerUsername));
    }

    /** Всички складови агенти в системата: от тях собственикът избира. */
    @Transactional(readOnly = true)
    public List<AppUser> findAllAgents() {
        return users.findByRole(Role.AGENT);
    }

    /** Склад по id, но само ако принадлежи на този собственик. */
    @Transactional(readOnly = true)
    public Warehouse getOwnedWarehouse(Long warehouseId, String ownerUsername) {
        Warehouse warehouse = warehouses.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Складът не е намерен."));

        if (!warehouse.getOwner().getUsername().equals(ownerUsername)) {
            log.warn("Потребител '{}' опита да достъпи чужд склад #{}", ownerUsername, warehouseId);
            throw new AccessDeniedException("Нямате достъп до този склад.");
        }
        // Агентите са LAZY: зареждаме ги още тук, докато транзакцията е отворена,
        // защото формата за редакция ги показва след това.
        Hibernate.initialize(warehouse.getAgents());
        return warehouse;
    }

    /** Създава нов склад. Новият склад винаги започва като свободен (AVAILABLE). */
    @Transactional
    public Warehouse create(WarehouseForm form, String ownerUsername) {
        AppUser owner = getOwner(ownerUsername);

        Warehouse warehouse = new Warehouse();
        form.applyTo(warehouse);
        warehouse.setOwner(owner);
        warehouse.setStatus(WarehouseStatus.AVAILABLE);
        warehouse.setAgents(resolveAgents(form.getAgentIds()));

        Warehouse saved = warehouses.save(warehouse);
        log.info("Собственик '{}' създаде склад #{} '{}' с агенти {}",
                ownerUsername, saved.getId(), saved.getName(), usernames(saved.getAgents()));
        return saved;
    }

    /** Редактира съществуващ склад на собственика, включително агентите му. */
    @Transactional
    public Warehouse update(Long warehouseId, WarehouseForm form, String ownerUsername) {
        Warehouse warehouse = getOwnedWarehouse(warehouseId, ownerUsername);
        Set<String> agentsBefore = usernames(warehouse.getAgents());

        form.applyTo(warehouse);
        warehouse.setAgents(resolveAgents(form.getAgentIds()));

        Warehouse saved = warehouses.save(warehouse);
        log.info("Собственик '{}' редактира склад #{} '{}'", ownerUsername, saved.getId(), saved.getName());

        Set<String> agentsAfter = usernames(saved.getAgents());
        if (!agentsBefore.equals(agentsAfter)) {
            log.info("Склад #{}: агентите са сменени от {} на {}", saved.getId(), agentsBefore, agentsAfter);
        }
        return saved;
    }

    /**
     * Превръща id-тата от формата в потребители и проверява,
     * че всеки от тях съществува и е складов агент.
     * Така през формата не може да се „назначи“ собственик или администратор.
     */
    private Set<AppUser> resolveAgents(Set<Long> agentIds) {
        if (agentIds == null || agentIds.isEmpty()) {
            return new HashSet<>();
        }
        List<AppUser> found = users.findAllById(agentIds);
        if (found.size() != agentIds.size()) {
            throw new IllegalArgumentException("Някой от избраните агенти не съществува.");
        }
        for (AppUser user : found) {
            if (user.getRole() != Role.AGENT) {
                throw new IllegalArgumentException(
                        "Потребителят '" + user.getUsername() + "' не е складов агент.");
            }
        }
        return new HashSet<>(found);
    }

    /** Имената на агентите, подредени по азбучен ред (за лога). */
    private static Set<String> usernames(Set<AppUser> agents) {
        return agents.stream()
                .map(AppUser::getUsername)
                .collect(Collectors.toCollection(TreeSet::new));
    }

    /** Намира потребителя и проверява, че е собственик. */
    private AppUser getOwner(String username) {
        AppUser user = users.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("Потребителят не е намерен."));
        if (user.getRole() != Role.OWNER) {
            throw new AccessDeniedException("Само собственик може да управлява складове.");
        }
        return user;
    }
}
