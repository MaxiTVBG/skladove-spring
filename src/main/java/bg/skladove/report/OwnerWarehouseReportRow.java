package bg.skladove.report;

import bg.skladove.rental.RentalContract;
import bg.skladove.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.List;

/**
 * Един ред от справката „Складове на собственик“ (задача №14):
 * складът и какво се е случило с него през избрания период.
 */
public class OwnerWarehouseReportRow {

    private final Warehouse warehouse;
    private final List<RentalContract> contracts;   // договорите, които се застъпват с периода
    private final long rentedDays;                  // колко дни от периода е бил нает
    private final long periodDays;                  // колко дни е периодът
    private final BigDecimal contractsTotal;        // сума на цените по тези договори

    public OwnerWarehouseReportRow(Warehouse warehouse, List<RentalContract> contracts,
                                   long rentedDays, long periodDays, BigDecimal contractsTotal) {
        this.warehouse = warehouse;
        this.contracts = contracts;
        this.rentedDays = rentedDays;
        this.periodDays = periodDays;
        this.contractsTotal = contractsTotal;
    }

    /** Заетост в проценти: наети дни / дни в периода. */
    public int getOccupancyPercent() {
        return periodDays == 0 ? 0 : (int) Math.round(rentedDays * 100.0 / periodDays);
    }

    public Warehouse getWarehouse() { return warehouse; }
    public List<RentalContract> getContracts() { return contracts; }
    public long getRentedDays() { return rentedDays; }
    public long getPeriodDays() { return periodDays; }
    public BigDecimal getContractsTotal() { return contractsTotal; }
}
