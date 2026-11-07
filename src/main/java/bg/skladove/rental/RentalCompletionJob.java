package bg.skladove.rental;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * Кога се извиква RentalCompletionService:
 *  - всяка минута (cron израз: секунда минута час ден месец ден-от-седмицата),
 *    така промените се виждат веднага, без рестарт на приложението;
 *  - веднъж при стартиране, за да се наваксат дните, в които приложението е било спряно.
 *
 * Проверката е лека: две заявки към базата, запис само ако нещо се е променило,
 * и ред в лога само при промяна.
 *
 * Работи, защото в SkladoveApplication има @EnableScheduling.
 * Логиката е в отделен service, за да мине през @Transactional proxy-то на Spring
 * и за да може да се тества без да се чака полунощ.
 */
@Component
public class RentalCompletionJob {

    private final RentalCompletionService completionService;

    public RentalCompletionJob(RentalCompletionService completionService) {
        this.completionService = completionService;
    }

    @Scheduled(cron = "0 * * * * *")
    public void everyMinute() {
        completionService.completeExpiredRentals(LocalDate.now());
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        completionService.completeExpiredRentals(LocalDate.now());
    }
}
