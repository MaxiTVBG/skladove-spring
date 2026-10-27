package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<Warehouse, Long> {

    List<Warehouse> findByOwner(AppUser owner);

    List<Warehouse> findByStatus(WarehouseStatus status);

    List<Warehouse> findByAgentsContaining(AppUser agent);
}
