package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Бизнес логика за складовете на собственика: създаване, редакция и преглед.
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

    /** Всички складове на даден собственик. */
    @Transactional(readOnly = true)
    public List<Warehouse> findByOwner(String ownerUsername) {
        return warehouses.findByOwner(getOwner(ownerUsername));
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

        Warehouse saved = warehouses.save(warehouse);
        log.info("Собственик '{}' създаде склад #{} '{}'", ownerUsername, saved.getId(), saved.getName());
        return saved;
    }

    /** Редактира съществуващ склад на собственика. */
    @Transactional
    public Warehouse update(Long warehouseId, WarehouseForm form, String ownerUsername) {
        Warehouse warehouse = getOwnedWarehouse(warehouseId, ownerUsername);
        form.applyTo(warehouse);

        Warehouse saved = warehouses.save(warehouse);
        log.info("Собственик '{}' редактира склад #{} '{}'", ownerUsername, saved.getId(), saved.getName());
        return saved;
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
