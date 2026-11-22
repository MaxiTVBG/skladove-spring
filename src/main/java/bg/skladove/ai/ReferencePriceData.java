package bg.skladove.ai;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Примерният набор от складове в resources/ai/reference-warehouse-prices.csv.
 *
 * Използва се заедно със складовете от базата. Без него в началото, когато
 * в системата има само няколко склада, моделът няма от какво да се учи.
 * Зарежда се веднъж при стартиране.
 */
@Component
public class ReferencePriceData {

    private static final Logger log = LogManager.getLogger(ReferencePriceData.class);
    static final String RESOURCE = "ai/reference-warehouse-prices.csv";

    private final List<PriceSample> samples;

    public ReferencePriceData() {
        this.samples = loadFromClasspath();
        log.info("Заредени примерни данни за препоръка на цена: {} склада", samples.size());
    }

    private ReferencePriceData(List<PriceSample> samples) {
        this.samples = List.copyOf(samples);
    }

    /** За тестове: набор с конкретни примери, без файла. */
    static ReferencePriceData of(List<PriceSample> samples) {
        return new ReferencePriceData(samples);
    }

    public List<PriceSample> samples() {
        return samples;
    }

    static List<PriceSample> loadFromClasspath() {
        try (Reader reader = new InputStreamReader(
                new ClassPathResource(RESOURCE).getInputStream(), StandardCharsets.UTF_8)) {
            return parse(reader);
        } catch (IOException e) {
            throw new UncheckedIOException("Липсва файлът " + RESOURCE, e);
        }
    }

    /**
     * Чете CSV с колони area,height,category,climate,monthly_price.
     * Редовете, започващи с #, са коментари; първият ред е заглавен.
     */
    static List<PriceSample> parse(Reader reader) throws IOException {
        List<PriceSample> result = new ArrayList<>();
        BufferedReader lines = new BufferedReader(reader);
        boolean header = true;
        String line;
        while ((line = lines.readLine()) != null) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }
            if (header) {
                header = false;
                continue;
            }
            String[] cols = line.split(",");
            PriceFeatures features = new PriceFeatures(
                    Double.valueOf(cols[0]),
                    cols[1].isBlank() ? null : Double.valueOf(cols[1]),
                    cols[2].isBlank() ? null : cols[2],
                    ClimateClass.fromText(cols[3]));
            result.add(new PriceSample(null, features, new BigDecimal(cols[4]), PriceSample.Source.REFERENCE));
        }
        return result;
    }
}
