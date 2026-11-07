
package bg.skladove.warehouse;

import bg.skladove.user.AppUser;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, Long> {

    // Намира складовете на конкретен собственик
    List<Warehouse> findByOwner(AppUser owner);

    // Същото, но зарежда и агентите с една заявка (за списъка „Моите складове“).
    // Без @EntityGraph агентите се зареждат мързеливо (LAZY) и страницата
    // не може да ги покаже, защото open-in-view е изключено.
    @EntityGraph(attributePaths = "agents")
    List<Warehouse> findWithAgentsByOwner(AppUser owner);

    // Намира складовете със зададен статус
    List<Warehouse> findByStatus(WarehouseStatus status);

    // Намира складовете, към които е назначен даден агент
    List<Warehouse> findByAgentsContaining(AppUser agent);
}
