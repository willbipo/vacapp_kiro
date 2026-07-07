package mx.vacapp.geography.internal.infrastructure.controllers.mobile.dtos;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO Response para estadísticas de rancho.
 * Contiene métricas agregadas y distribución por sección.
 */
public record RanchoStatsResponse(
    int totalSecciones,
    int totalPotreros,
    BigDecimal superficieTotal,
    BigDecimal superficieUsada,
    BigDecimal superficieDisponible,
    BigDecimal porcentajeUso,
    List<SeccionDistributionDto> distribucionPorSeccion
) {
    /**
     * DTO para distribución de superficie por sección.
     */
    public record SeccionDistributionDto(
        String nombreSeccion,
        BigDecimal superficie,
        BigDecimal porcentaje,
        int totalPotreros
    ) {
    }
}
