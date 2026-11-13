
package bg.skladove.notification;

import bg.skladove.rental.RentalContract;
import bg.skladove.rental.RentalContractRepository;
import bg.skladove.rental.RentalStatus;
import bg.skladove.user.AppUser;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Service
public class ContractExpirationNotificationService {

    private static final ZoneId BULGARIA_ZONE =
            ZoneId.of("Europe/Sofia");

    private static final int DAYS_BEFORE_EXPIRATION = 7;

    private final RentalContractRepository contractRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;

    public ContractExpirationNotificationService(
            RentalContractRepository contractRepository,
            NotificationRepository notificationRepository,
            NotificationService notificationService) {

        this.contractRepository = contractRepository;
        this.notificationRepository = notificationRepository;
        this.notificationService = notificationService;
    }

    // Runs every day at 08:00 Bulgarian time
    @Scheduled(
            cron = "0 0 8 * * *",
            zone = "Europe/Sofia"
    )
    @Transactional
    public void notifyAboutExpiringContracts() {

        LocalDate expirationDate =
                LocalDate.now(BULGARIA_ZONE)
                        .plusDays(DAYS_BEFORE_EXPIRATION);

        List<RentalContract> contracts =
                contractRepository.findByEndDateBetween(
                        expirationDate, expirationDate);

        for (RentalContract contract : contracts) {

            if (contract.getStatus() != RentalStatus.ACTIVE) {
                continue;
            }

            AppUser owner = contract.getWarehouse().getOwner();
            AppUser agent = contract.getAgent();

            String message =
                    "Договор #" + contract.getId()
                            + " за склад „"
                            + contract.getWarehouse().getName()
                            + "“ изтича след 7 дни ("
                            + contract.getEndDate() + ").";

            createIfNotExists(owner, message);
            createIfNotExists(agent, message);
        }
    }

    private void createIfNotExists(
            AppUser recipient,
            String message) {

        boolean alreadyExists =
                notificationRepository
                        .existsByRecipientAndTypeAndMessage(
                                recipient,
                                Notification.Type.CONTRACT_EXPIRING,
                                message);

        if (!alreadyExists) {
            notificationService.createNotification(
                    recipient,
                    Notification.Type.CONTRACT_EXPIRING,
                    message);
        }
    }
}
