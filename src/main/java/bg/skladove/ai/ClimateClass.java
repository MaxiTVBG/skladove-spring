package bg.skladove.ai;

import java.util.Locale;

/**
 * Климатичните условия, сведени до четири класа.
 *
 * В склада климатът е свободен текст („Сух“, „Хладилен (0–8 °C)“ и т.н.),
 * а моделът има нужда от стойност, която може да се сравнява.
 * Класовете са подредени по това колко скъпо е поддържането на условията,
 * затова разстоянието между тях е разликата в поредните номера:
 * „сух“ и „фризер“ са най-далеч един от друг.
 */
public enum ClimateClass {

    DRY("Сух"),
    HEATED("Отопляем"),
    COOLED("Хладилен"),
    FROZEN("Фризер");

    private final String label;

    ClimateClass(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    /**
     * Разпознава класа по ключови думи в текста. Връща null, ако текстът е празен
     * или не прилича на нито един клас; тогава моделът не взима климата предвид.
     */
    public static ClimateClass fromText(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("фриз") || t.contains("замраз") || t.contains("-18")) {
            return FROZEN;
        }
        if (t.contains("хлад") || t.contains("охлажд")) {
            return COOLED;
        }
        if (t.contains("отопл") || t.contains("топъл")) {
            return HEATED;
        }
        if (t.contains("сух")) {
            return DRY;
        }
        return null;
    }
}
