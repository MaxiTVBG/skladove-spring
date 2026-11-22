package bg.skladove.ai;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Изискванията на клиента, които агентът въвежда, за да получи препоръка на склад.
 * Задължителни са площта и периодът; останалите може да липсват (null).
 *
 * @param budget максимална месечна цена
 */
public record WarehouseRequest(Double area,
                               ClimateClass climate,
                               String goodsType,
                               String category,
                               BigDecimal budget,
                               LocalDate from,
                               LocalDate to) {
}
