package bg.skladove.rental;

import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import bg.skladove.warehouse.WarehouseStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Поддържа статусите актуални с течение на времето:
 *  1) активен договор, чиято крайна дата е минала, става COMPLETED
 *     (след това собственикът може да оцени агента, задача №6);
 *  2) статусът на склада се пресмята отново: RENTED, ако днес има активен
 *     договор, иначе AVAILABLE. Така одобрен договор за бъдещ период прави
 *     склада „Нает“ точно в деня, в който започва.
 *
 * Извиква се всяка нощ и при стартиране на приложението (виж RentalCompletionJob).
 */
@Service
public class RentalCompletionService {

    private static final Logger log = LogManager.getLogger(RentalCompletionService.class);

    private final RentalContractRepository contracts;
    private final WarehouseRepository warehouses;
    private final WarehouseAvailabilityService availability;

    public RentalCompletionService(RentalContractRepository contracts,
                                   WarehouseRepository warehouses,
                                   WarehouseAvailabilityService availability) {
        this.contracts = contracts;
        this.warehouses = warehouses;
        this.availability = availability;
    }

    /**
     * @param today днешната дата (подава се отвън, за да може тестът да „върти“ времето)
     * @return колко договора са приключени
     */
    @Transactional
    public int completeExpiredRentals(LocalDate today) {
        // Складовете, чийто статус трябва да се пресметне отново
        Set<Warehouse> toSynchronize = new LinkedHashSet<>();

        // 1) Активни договори с крайна дата преди днес -> COMPLETED
        List<RentalContract> expired =
                contracts.findByStatusAndEndDateBefore(RentalStatus.ACTIVE, today);
        for (RentalContract contract : expired) {
            contract.setStatus(RentalStatus.COMPLETED);
            toSynchronize.add(contract.getWarehouse());
            log.info("Договор #{} за склад '{}' е приключил (край {})",
                    contract.getId(), contract.getWarehouse().getName(), contract.getEndDate());
        }
        contracts.saveAll(expired);

        // 2) Складовете с активни договори: някой може да започва днес
        for (RentalContract active : contracts.findByStatus(RentalStatus.ACTIVE)) {
            toSynchronize.add(active.getWarehouse());
        }

        for (Warehouse warehouse : toSynchronize) {
            WarehouseStatus before = warehouse.getStatus();
            availability.synchronizeStatus(warehouse);
            warehouses.save(warehouse);
            if (before != warehouse.getStatus()) {
                log.info("Склад #{} '{}': статус {} -> {}",
                        warehouse.getId(), warehouse.getName(), before, warehouse.getStatus());
            }
        }

        if (!expired.isEmpty()) {
            log.info("Приключени договори: {}", expired.size());
        }
        return expired.size();
    }
}
