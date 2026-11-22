package bg.skladove.ai;

import bg.skladove.warehouse.Warehouse;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Оценява доколко склад отговаря на изискванията на клиента (препоръчваща система по съдържание).
 *
 * Всеки критерий дава оценка от 0 до 1, а общото съвпадение е претеглена сума:
 * <pre>
 *   площ 30% · климат 30% · бюджет 25% · вид стока 10% · категория 5%
 * </pre>
 * Освен числото се връщат и причините („в рамките на бюджета“, „над бюджета с 120 лв.“),
 * така че агентът вижда защо складът е препоръчан.
 *
 * Класът не зависи от Spring и базата, затова се тества с обикновени JUnit тестове.
 */
final class WarehouseScorer {

    static final double WEIGHT_AREA = 0.30;
    static final double WEIGHT_CLIMATE = 0.30;
    static final double WEIGHT_BUDGET = 0.25;
    static final double WEIGHT_GOODS = 0.10;
    static final double WEIGHT_CATEGORY = 0.05;

    /**
     * Доколко склад с даден климат става за стока, която изисква друг климат.
     * Ред: нужен климат, колона: климат на склада (сух, отопляем, хладилен, фризер).
     * 0 означава, че стоката ще се развали — такъв склад не се препоръчва изобщо.
     */
    private static final double[][] CLIMATE_FIT = {
            //  сух  отопл. хлад. фризер
            {1.0, 0.8, 0.4, 0.2},   // нужен сух
            {0.5, 1.0, 0.2, 0.1},   // нужен отопляем
            {0.0, 0.0, 1.0, 0.6},   // нужен хладилен
            {0.0, 0.0, 0.0, 1.0}    // нужен фризер
    };

    private WarehouseScorer() {
    }

    /** Резултат от оценката на един склад. */
    record Score(int percent, boolean suitable, List<String> pros, List<String> cons) {
    }

    static Score score(WarehouseRequest request, Warehouse w) {
        List<String> pros = new ArrayList<>();
        List<String> cons = new ArrayList<>();

        double area = areaScore(request.area(), w.getArea());
        if (w.getArea() == null) {
            cons.add("Не е посочена площ");
        } else if (w.getArea() < request.area()) {
            cons.add(String.format("Площта е недостатъчна: %s м² при нужни %s м²", num(w.getArea()), num(request.area())));
        } else {
            pros.add(String.format("Достатъчна площ: %s м²", num(w.getArea())));
            if (w.getArea() > 2 * request.area()) {
                cons.add("Складът е много по-голям от нужното");
            }
        }

        ClimateClass has = ClimateClass.fromText(w.getClimateConditions());
        double climate = climateScore(request.climate(), has);
        if (request.climate() != null) {
            if (has == null) {
                cons.add("Не е посочен климат");
            } else if (climate == 1.0) {
                pros.add("Подходящ климат: " + has.getLabel());
            } else if (climate > 0) {
                cons.add("Климатът е различен (" + has.getLabel() + "), но е приемлив");
            }
        }

        double budget = budgetScore(request.budget(), w.getMonthlyPrice());
        if (request.budget() != null && w.getMonthlyPrice() != null) {
            BigDecimal over = w.getMonthlyPrice().subtract(request.budget());
            if (over.signum() <= 0) {
                pros.add("В рамките на бюджета");
            } else {
                cons.add("Над бюджета с " + over.stripTrailingZeros().toPlainString() + " лв.");
            }
        }

        double goods = goodsScore(request.goodsType(), w.getGoodsType());
        if (!isBlank(request.goodsType()) && goods >= 0.5) {
            pros.add("Предназначен за: " + w.getGoodsType());
        }

        double category = categoryScore(request.category(), w.getCategory());
        if (!isBlank(request.category()) && category == 1.0) {
            pros.add("Категория " + w.getCategory());
        }

        double total = WEIGHT_AREA * area + WEIGHT_CLIMATE * climate + WEIGHT_BUDGET * budget
                + WEIGHT_GOODS * goods + WEIGHT_CATEGORY * category;
        boolean suitable = climate > 0;
        return new Score((int) Math.round(total * 100), suitable, pros, cons);
    }

    /**
     * Точно колкото трябва = 1. По-голям склад губи плавно (4 пъти по-голям = 0.5),
     * защото клиентът ще плаща за излишна площ. По-малък губи рязко, защото стоката няма да се събере.
     */
    static double areaScore(double needed, Double area) {
        if (area == null || area <= 0) {
            return 0.5;
        }
        if (area >= needed) {
            return Math.sqrt(needed / area);
        }
        double ratio = area / needed;
        return 0.5 * ratio * ratio;
    }

    static double climateScore(ClimateClass needed, ClimateClass has) {
        if (needed == null) {
            return 1.0;
        }
        if (has == null) {
            return 0.5;
        }
        return CLIMATE_FIT[needed.ordinal()][has.ordinal()];
    }

    /** В бюджета = 1; над бюджета оценката пада и при 50% надвишение е 0. */
    static double budgetScore(BigDecimal budget, BigDecimal price) {
        if (budget == null || budget.signum() <= 0) {
            return 1.0;
        }
        if (price == null) {
            return 0.5;
        }
        if (price.compareTo(budget) <= 0) {
            return 1.0;
        }
        double over = price.subtract(budget).doubleValue() / budget.doubleValue();
        return Math.max(0, 1 - 2 * over);
    }

    /**
     * Сходство на текста „вид стока“: каква част от думите на клиента се срещат при склада.
     * Думите се сравняват по първите 5 букви, за да съвпаднат „храни“ и „хранителни“.
     */
    static double goodsScore(String needed, String has) {
        if (isBlank(needed)) {
            return 1.0;
        }
        if (isBlank(has)) {
            return 0.5;
        }
        Set<String> need = stems(needed);
        if (need.isEmpty()) {
            return 1.0;
        }
        Set<String> have = stems(has);
        long common = need.stream().filter(have::contains).count();
        return (double) common / need.size();
    }

    static double categoryScore(String needed, String has) {
        if (isBlank(needed)) {
            return 1.0;
        }
        if (isBlank(has)) {
            return 0.5;
        }
        return needed.equalsIgnoreCase(has) ? 1.0 : 0.3;
    }

    private static Set<String> stems(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}]+"))
                .filter(word -> word.length() >= 3)
                .map(word -> word.length() > 5 ? word.substring(0, 5) : word)
                .collect(Collectors.toSet());
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String num(double value) {
        return value == Math.floor(value) ? String.valueOf((long) value) : String.valueOf(value);
    }
}
