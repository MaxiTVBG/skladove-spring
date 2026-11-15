package bg.skladove.report;

import bg.skladove.warehouse.Warehouse;

import java.time.LocalDate;

/**
 * Един ред от справката „Налични складове“: складът и в какви граници е свободен.
 *
 * freeSince – от коя дата е свободен (денят след края на последния договор
 *             преди периода); null означава „свободен и преди периода“.
 * freeUntil – до коя дата остава свободен (денят преди следващия договор
 *             след периода); null означава „няма следващ договор“.
 */
public class AvailableWarehouse {

    private final Warehouse warehouse;
    private final LocalDate freeSince;
    private final LocalDate freeUntil;

    public AvailableWarehouse(Warehouse warehouse, LocalDate freeSince, LocalDate freeUntil) {
        this.warehouse = warehouse;
        this.freeSince = freeSince;
        this.freeUntil = freeUntil;
    }

    public Warehouse getWarehouse() { return warehouse; }
    public LocalDate getFreeSince() { return freeSince; }
    public LocalDate getFreeUntil() { return freeUntil; }
}
