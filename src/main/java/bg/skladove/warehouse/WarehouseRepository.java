
package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, Long> {

    // Намира складовете на конкретен собственик
    List<Warehouse> findByOwner(AppUser owner);

    // Намира складовете със зададен статус
    List<Warehouse> findByStatus(WarehouseStatus status);

    // Намира складовете, към които е назначен даден агент
    List<Warehouse> findByAgentsContaining(AppUser agent);
}
