package bg.skladove.ai;

/**
 * Резултат от проверката на модела с leave-one-out:
 * всеки склад се „скрива“, цената му се прогнозира от останалите и се сравнява с истинската.
 *
 * @param k                брой съседи
 * @param sampleCount      колко склада са проверени
 * @param meanAbsoluteError        средна грешка в лева (MAE)
 * @param meanAbsolutePercentError средна грешка в проценти (MAPE)
 * @param baselineAbsoluteError        MAE на базовия метод (медианна цена на м² × площ)
 * @param baselineAbsolutePercentError MAPE на базовия метод
 */
public record ModelEvaluation(int k,
                              int sampleCount,
                              double meanAbsoluteError,
                              double meanAbsolutePercentError,
                              double baselineAbsoluteError,
                              double baselineAbsolutePercentError) {
}
