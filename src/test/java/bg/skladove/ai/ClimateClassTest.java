package bg.skladove.ai;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ClimateClassTest {

    @Test
    void recognisesTheOptionsFromTheForm() {
        assertThat(ClimateClass.fromText("Сух")).isEqualTo(ClimateClass.DRY);
        assertThat(ClimateClass.fromText("Отопляем")).isEqualTo(ClimateClass.HEATED);
        assertThat(ClimateClass.fromText("Хладилен (0–8 °C)")).isEqualTo(ClimateClass.COOLED);
        assertThat(ClimateClass.fromText("Фризер (под -18 °C)")).isEqualTo(ClimateClass.FROZEN);
    }

    @Test
    void recognisesFreeTextIgnoringCase() {
        assertThat(ClimateClass.fromText("сухо и проветриво")).isEqualTo(ClimateClass.DRY);
        assertThat(ClimateClass.fromText("ОХЛАЖДАН")).isEqualTo(ClimateClass.COOLED);
        assertThat(ClimateClass.fromText("камера за замразяване")).isEqualTo(ClimateClass.FROZEN);
    }

    @Test
    void unknownOrEmptyTextGivesNull() {
        assertThat(ClimateClass.fromText(null)).isNull();
        assertThat(ClimateClass.fromText("  ")).isNull();
        assertThat(ClimateClass.fromText("нормален")).isNull();
    }
}
