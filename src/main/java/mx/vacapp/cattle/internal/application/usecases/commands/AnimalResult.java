package mx.vacapp.cattle.internal.application.usecases.commands;

import mx.vacapp.cattle.internal.domain.model.Animal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Resultado inmutable que representa los datos de un animal.
 * Record utilizado para retornar información de animal desde casos de uso hacia controladores.
 * Incluye todos los campos de la entidad de dominio Animal más campos enriquecidos
 * calculados en tiempo real (meses, pesoActual, potreroActual, nombres de padres).
 */
public record AnimalResult(
    UUID animalId,
    String arete,
    String areteAnterior,
    String sexo,
    String raza,
    LocalDate fechaNacimiento,
    Integer meses,
    LocalDate fechaAretado,
    String tipo,
    String status,
    String folioReemo,
    String nota,
    UUID madreId,
    UUID padreId,
    UUID ranchoId,
    UUID tenantId,
    Instant createdAt,
    Instant updatedAt,
    UUID createdBy,
    UUID updatedBy,
    LocalDate fechaVenta,
    BigDecimal precioVenta,
    LocalDate fechaMuerte,
    String motivoMuerte,
    // Campos enriquecidos (calculados en tiempo real)
    BigDecimal pesoActual,
    UUID potreroActual,
    String nombreMadre,
    String nombrePadre
) {
    /**
     * Método factory para crear un AnimalResult desde una entidad de dominio Animal.
     * Convierte el objeto de dominio en un DTO inmutable para la capa de aplicación.
     * Los enums se convierten a String usando getValue() para mantener consistencia con la API.
     * Los campos enriquecidos se establecen como null (deben calcularse con fromDomainEnriched).
     *
     * @param animal la entidad de dominio Animal
     * @return un nuevo AnimalResult con los datos del animal (campos enriquecidos en null)
     */
    public static AnimalResult fromDomain(Animal animal) {
        return new AnimalResult(
            animal.getAnimalId(),
            animal.getArete(),
            animal.getAreteAnterior(),
            animal.getSexo().getValue(),
            animal.getRaza().getValue(),
            animal.getFechaNacimiento(),
            animal.getMeses(),
            animal.getFechaAretado(),
            animal.getTipo().getValue(),
            animal.getStatus().getValue(),
            animal.getFolioReemo(),
            animal.getNota(),
            animal.getMadreId(),
            animal.getPadreId(),
            animal.getRanchoId(),
            animal.getTenantId(),
            animal.getCreatedAt(),
            animal.getUpdatedAt(),
            animal.getCreatedBy(),
            animal.getUpdatedBy(),
            animal.getFechaVenta(),
            animal.getPrecioVenta(),
            animal.getFechaMuerte(),
            animal.getMotivoMuerte(),
            null,  // pesoActual
            null,  // potreroActual
            null,  // nombreMadre
            null   // nombrePadre
        );
    }
    
    /**
     * Método factory para crear un AnimalResult enriquecido con campos calculados.
     * Este método incluye datos adicionales obtenidos de repositorios relacionados.
     *
     * @param animal la entidad de dominio Animal
     * @param mesesActuales edad en meses calculada en tiempo real
     * @param pesoActual peso actual (último peso registrado) o null si no tiene pesos
     * @param potreroActual UUID del potrero actual o null si no tiene ubicación
     * @param nombreMadre arete de la madre o null si no tiene madre
     * @param nombrePadre arete del padre o null si no tiene padre
     * @return un nuevo AnimalResult con todos los datos enriquecidos
     */
    public static AnimalResult fromDomainEnriched(
            Animal animal,
            Integer mesesActuales,
            BigDecimal pesoActual,
            UUID potreroActual,
            String nombreMadre,
            String nombrePadre) {
        return new AnimalResult(
            animal.getAnimalId(),
            animal.getArete(),
            animal.getAreteAnterior(),
            animal.getSexo().getValue(),
            animal.getRaza().getValue(),
            animal.getFechaNacimiento(),
            mesesActuales,  // Usar la edad calculada en tiempo real
            animal.getFechaAretado(),
            animal.getTipo().getValue(),
            animal.getStatus().getValue(),
            animal.getFolioReemo(),
            animal.getNota(),
            animal.getMadreId(),
            animal.getPadreId(),
            animal.getRanchoId(),
            animal.getTenantId(),
            animal.getCreatedAt(),
            animal.getUpdatedAt(),
            animal.getCreatedBy(),
            animal.getUpdatedBy(),
            animal.getFechaVenta(),
            animal.getPrecioVenta(),
            animal.getFechaMuerte(),
            animal.getMotivoMuerte(),
            pesoActual,
            potreroActual,
            nombreMadre,
            nombrePadre
        );
    }
}
