package bg.skladove.ai;

import java.math.BigDecimal;
import java.util.List;

/**
 * Резултат от препоръката за цена. Връща се като JSON към формата за склад.
 *
 * @param recommendedPrice препоръчана месечна цена
 * @param minPrice         най-ниската цена, ако складът се оцени като най-евтиния от съседите
 * @param maxPrice         най-високата цена, ако складът се оцени като най-скъпия от съседите
 * @param pricePerSquareMeter препоръчана цена на м²
 * @param neighbors        най-подобните складове, от които е изчислена цената
 * @param trainingSize     от колко склада общо е избирал моделът
 */
public record PriceRecommendation(BigDecimal recommendedPrice,
                                  BigDecimal minPrice,
                                  BigDecimal maxPrice,
                                  BigDecimal pricePerSquareMeter,
                                  List<Neighbor> neighbors,
                                  int trainingSize) {

    /** Един от най-подобните складове. */
    public record Neighbor(String source,
                           Double area,
                           Double height,
                           String category,
                           String climate,
                           BigDecimal monthlyPrice,
                           BigDecimal pricePerSquareMeter,
                           int similarityPercent) {
    }
}
