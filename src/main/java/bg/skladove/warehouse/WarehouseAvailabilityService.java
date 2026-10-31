
package bg.skladove.warehouse;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class WarehouseAvailabilityService {

    private final RentalContractRepository contractRepository;

    public WarehouseAvailabilityService(
            RentalContractRepository contractRepository) {
        this.contractRepository = contractRepository;
    }

    // Проверява дали складът е свободен за даден период.
    public boolean isAvailable(
            Warehouse warehouse,
            LocalDate startDate,
            LocalDate endDate) {

        if (warehouse.getStatus() != WarehouseStatus.AVAILABLE
                && warehouse.getStatus() != WarehouseStatus.RENTED) {
            return false;
        }

        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {
            return false;
        }

        List<RentalContract> activeContracts =
                contractRepository.findByWarehouseIdAndStatus(
                        warehouse.getId(),
                        RentalStatus.ACTIVE
                );

        return activeContracts.stream().noneMatch(contract ->
                !startDate.isAfter(contract.getEndDate())
                        && !endDate.isBefore(contract.getStartDate())
        );
    }

    // Актуализира общия статус на склада според активните договори.
    public void synchronizeStatus(Warehouse warehouse) {

        if (warehouse.getStatus() == WarehouseStatus.MAINTENANCE
                || warehouse.getStatus() == WarehouseStatus.INACTIVE) {
            return;
        }

        LocalDate today = LocalDate.now();

        List<RentalContract> activeContracts =
                contractRepository.findByWarehouseIdAndStatus(
                        warehouse.getId(),
                        RentalStatus.ACTIVE
                );

        boolean currentlyRented = activeContracts.stream().anyMatch(
                contract ->
                        !today.isBefore(contract.getStartDate())
                                && !today.isAfter(contract.getEndDate())
        );

        warehouse.setStatus(currentlyRented
                ? WarehouseStatus.RENTED
                : WarehouseStatus.AVAILABLE);
    }
}
