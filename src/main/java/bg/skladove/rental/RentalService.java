
package bg.skladove.rental;

import bg.skladove.user.AppUser;
import bg.skladove.user.AppUserRepository;
import bg.skladove.user.Role;
import bg.skladove.warehouse.Warehouse;
import bg.skladove.warehouse.WarehouseAvailabilityService;
import bg.skladove.warehouse.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class RentalService {

    private final RentalContractRepository contractRepository;
    private final WarehouseRepository warehouseRepository;
    private final AppUserRepository userRepository;
    private final WarehouseAvailabilityService availabilityService;

    public RentalService(
            RentalContractRepository contractRepository,
            WarehouseRepository warehouseRepository,
            AppUserRepository userRepository,
            WarehouseAvailabilityService availabilityService) {

        this.contractRepository = contractRepository;
        this.warehouseRepository = warehouseRepository;
        this.userRepository = userRepository;
        this.availabilityService = availabilityService;
    }

    // Agent creates a rental request
    @Transactional
    public RentalContract createRequest(
            Long warehouseId,
            Long agentId,
            String tenantName,
            String tenantPhone,
            String tenantEmail,
            LocalDate startDate,
            LocalDate endDate,
            BigDecimal price) {

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Складът не е намерен."));

        AppUser agent = userRepository.findById(agentId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Агентът не е намерен."));

        if (agent.getRole() != Role.AGENT) {
            throw new IllegalArgumentException(
                    "Само агент може да създава заявка.");
        }

        if (!warehouse.getAgents().contains(agent)) {
            throw new IllegalArgumentException(
                    "Агентът не е назначен към този склад.");
        }

        if (tenantName == null || tenantName.isBlank()) {
            throw new IllegalArgumentException(
                    "Името на наемателя е задължително.");
        }

        if (startDate == null || endDate == null
                || endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "Невалиден период на наемане.");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Цената трябва да бъде положителна.");
        }

        if (!availabilityService.isAvailable(
                warehouse, startDate, endDate)) {
            throw new IllegalStateException(
                    "Складът не е свободен за избрания период.");
        }

        RentalContract contract = new RentalContract();
        contract.setWarehouse(warehouse);
        contract.setAgent(agent);
        contract.setTenantName(tenantName);
        contract.setTenantPhone(tenantPhone);
        contract.setTenantEmail(tenantEmail);
        contract.setStartDate(startDate);
        contract.setEndDate(endDate);
        contract.setPrice(price);
        contract.setStatus(RentalStatus.PENDING_OWNER_APPROVAL);

        return contractRepository.save(contract);
    }

    // Owner approves a rental request
    @Transactional
    public RentalContract approveRequest(
            Long contractId, Long ownerId) {

        RentalContract contract = getContract(contractId);
        Warehouse warehouse = contract.getWarehouse();

        verifyOwner(warehouse, ownerId);
        verifyPending(contract);

        if (!availabilityService.isAvailable(
                warehouse,
                contract.getStartDate(),
                contract.getEndDate())) {
            throw new IllegalStateException(
                    "Складът вече не е свободен за този период.");
        }

        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Собственикът не е намерен."));

        contract.setStatus(RentalStatus.ACTIVE);
        contract.setDecidedBy(owner);
        contract.setDecidedAt(LocalDateTime.now());

        contractRepository.save(contract);

        availabilityService.synchronizeStatus(warehouse);
        warehouseRepository.save(warehouse);

        return contract;
    }

    // Owner rejects a rental request
    @Transactional
    public RentalContract rejectRequest(
            Long contractId, Long ownerId, String note) {

        RentalContract contract = getContract(contractId);

        verifyOwner(contract.getWarehouse(), ownerId);
        verifyPending(contract);

        AppUser owner = userRepository.findById(ownerId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Собственикът не е намерен."));

        contract.setStatus(RentalStatus.REJECTED);
        contract.setDecidedBy(owner);
        contract.setDecidedAt(LocalDateTime.now());
        contract.setDecisionNote(note);

        return contractRepository.save(contract);
    }

    private RentalContract getContract(Long contractId) {
        return contractRepository.findById(contractId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Договорът не е намерен."));
    }

    private void verifyOwner(Warehouse warehouse, Long ownerId) {
        if (!warehouse.getOwner().getId().equals(ownerId)) {
            throw new IllegalArgumentException(
                    "Нямате право да одобрявате този договор.");
        }
    }

    private void verifyPending(RentalContract contract) {
        if (contract.getStatus()
                != RentalStatus.PENDING_OWNER_APPROVAL) {
            throw new IllegalStateException(
                    "Заявката вече е обработена.");
        }
    }
}
