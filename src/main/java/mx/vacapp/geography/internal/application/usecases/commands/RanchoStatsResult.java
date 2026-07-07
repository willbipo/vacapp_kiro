package mx.vacapp.geography.internal.application.usecases.commands;

import java.math.BigDecimal;
import java.util.List;

/**
 * Resultado con estadísticas calculadas de un rancho.
 * Incluye métricas agregadas y distribución por sección.
 */
public record RanchoStatsResult(
    int totalSecciones,
    int totalPotreros,
    BigDecimal superficieTotal,
    BigDecimal superficieUsada,
    BigDecimal superficieDisponible,
    BigDecimal porcentajeUso,
    List<SeccionDistribution> distribucionPorSeccion
) {
    /**
     * Distribución de superficie por sección.
     */
    public record SeccionDistribution(
        String nombreSeccion,
        BigDecimal superficie,
        BigDecimal porcentaje,
        int totalPotreros
    ) {
    }
}
