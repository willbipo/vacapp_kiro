package mx.vacapp.cattle.internal.infrastructure.persistence.impl;

import lombok.RequiredArgsConstructor;
import mx.vacapp.cattle.internal.domain.model.PastureHistory;
import mx.vacapp.cattle.internal.domain.repository.PastureHistoryRepository;
import mx.vacapp.cattle.internal.infrastructure.persistence.entities.PastureHistoryEntity;
import mx.vacapp.cattle.internal.infrastructure.persistence.mappers.PastureHistoryMapper;
import mx.vacapp.cattle.internal.infrastructure.persistence.repositories.PastureHistoryJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementación del puerto de salida PastureHistoryRepository usando Spring Data JPA.
 * 
 * Esta clase es el adaptador de infraestructura que conecta la capa de dominio
 * (PastureHistoryRepository interface) con la tecnología de persistencia concreta
 * (JPA/Hibernate + MySQL).
 * 
 * Responsabilidades:
 * - Implementar todos los métodos del puerto PastureHistoryRepository
 * - Transformar entre entidades de dominio (PastureHistory) y entidades JPA (PastureHistoryEntity)
 * - Delegar operaciones CRUD al PastureHistoryJpaRepository
 * - Garantizar integridad de datos (un animal = máximo 1 ubicación actual)
 * 
 * NO gestiona multi-tenancy porque el historial de potreros está asociado
 * a animales que ya tienen tenant_id. La validación de tenant se hace al
 * nivel del Animal repository.
 * 
 * Patrón de diseño:
 * - Repository Pattern (implementación de puerto de salida)
 * - Adapter Pattern (adapta JPA a interfaz de dominio)
 * - Clean Architecture (infraestructura implementa contratos de dominio)
 * 
 * @see PastureHistoryRepository puerto de dominio
 * @see PastureHistoryJpaRepository repositorio JPA
 * @see PastureHistoryMapper transformación de entidades
 */
@Repository
@RequiredArgsConstructor
public class PastureHistoryRepositoryImpl implements PastureHistoryRepository {
    
    private final PastureHistoryJpaRepository jpaRepository;
    
    /**
     * Inserta un nuevo registro de entrada de animal a potrero.
     * 
     * Guarda un nuevo registro con fecha_entrada y fecha_salida = null,
     * representando que el animal acaba de entrar al potrero.
     * 
     * IMPORTANTE: No valida si ya existe un registro actual (fecha_salida = null)
     * para el mismo animal. El constraint UNIQUE en la base de datos
     * (animal_id, fecha_salida) lanzará excepción si se intenta insertar
     * duplicado. El caso de uso debe cerrar la ubicación anterior antes
     * de llamar a este método.
     * 
     * @param history registro de historial con fecha_salida = null
     * @return el registro persistido con ID generado
     * @throws IllegalArgumentException si history es null
     * @throws org.springframework.dao.DataIntegrityViolationException
     *         si ya existe un registro actual para el mismo animal
     */
    @Override
    @Transactional
    public PastureHistory insert(PastureHistory history) {
        if (history == null) {
            throw new IllegalArgumentException("El registro de historial no puede ser null");
        }
        
        // Transformar dominio → JPA
        PastureHistoryEntity entity = PastureHistoryMapper.toEntity(history);
        
        // Persistir
        PastureHistoryEntity savedEntity = jpaRepository.save(entity);
        
        // Transformar JPA → dominio
        return PastureHistoryMapper.toDomain(savedEntity);
    }
    
    /**
     * Busca el registro actual (ubicación actual) de un animal.
     * 
     * Retorna el registro donde fecha_salida = null, indicando que
     * el animal aún está en ese potrero.
     * 
     * @param animalId UUID del animal
     * @return Optional con el registro actual si el animal está en un potrero,
     *         empty si el animal no tiene ubicación actual (vendido/muerto/sin asignar)
     */
    @Override
    @Transactional(readOnly = true)
    public Optional<PastureHistory> findCurrentByAnimalId(UUID animalId) {
        return jpaRepository.findByAnimalIdAndFechaSalidaIsNull(animalId)
                .map(PastureHistoryMapper::toDomain);
    }
    
    /**
     * Actualiza la fecha de salida del registro actual de un animal.
     * 
     * Cierra la ubicación actual estableciendo fecha_salida = :fechaSalida.
     * Esto indica que el animal salió del potrero en ese momento.
     * 
     * IMPORTANTE: Este método actualiza el registro actual (fecha_salida = null)
     * del animal. Si el animal no tiene ubicación actual, no se actualiza nada.
     * 
     * Flujo típico de movimiento:
     * 1. updateFechaSalida(animalId, NOW()) - cierra ubicación actual
     * 2. insert(new PastureHistory(...)) - abre nueva ubicación
     * 
     * @param animalId UUID del animal cuya ubicación actual se cerrará
     * @param fechaSalida fecha/hora de salida del potrero
     * @throws IllegalArgumentException si animalId es null o fechaSalida es null
     */
    @Override
    @Transactional
    public void updateFechaSalida(UUID animalId, Instant fechaSalida) {
        if (animalId == null) {
            throw new IllegalArgumentException("El animalId no puede ser null");
        }
        if (fechaSalida == null) {
            throw new IllegalArgumentException("La fecha de salida no puede ser null");
        }
        
        // Ejecutar UPDATE en el registro actual (fecha_salida = null)
        jpaRepository.updateFechaSalidaByAnimalId(animalId, fechaSalida);
        
        // Nota: El método @Modifying retorna int (cantidad de registros actualizados)
        // pero no validamos el resultado aquí. Si retorna 0, significa que el animal
        // no tenía ubicación actual, lo cual es válido (caso: animal vendido/muerto).
    }
    
    /**
     * Obtiene el historial completo de movimientos de un animal.
     * 
     * Retorna todos los registros (actuales e históricos) ordenados cronológicamente
     * de más reciente a más antiguo (fecha_entrada DESC).
     * 
     * Incluye:
     * - Registro actual (fecha_salida = null) si existe
     * - Todos los registros históricos (fecha_salida != null)
     * 
     * @param animalId UUID del animal
     * @return lista ordenada de registros de historial (más reciente primero),
     *         lista vacía si el animal nunca ha estado en un potrero
     */
    @Override
    @Transactional(readOnly = true)
    public List<PastureHistory> findHistoryByAnimalId(UUID animalId) {
        return jpaRepository.findByAnimalIdOrderByFechaEntradaDesc(animalId)
                .stream()
                .map(PastureHistoryMapper::toDomain)
                .collect(Collectors.toList());
    }
    
    /**
     * Lista todos los animales actualmente en un potrero específico.
     * 
     * Retorna los IDs de animales que tienen un registro con:
     * - potrero_id = potreroId especificado
     * - fecha_salida = null (aún están en el potrero)
     * 
     * Útil para:
     * - Listar animales en un potrero
     * - Contar cuántos animales hay en un potrero
     * - Validar disponibilidad de espacio en un potrero
     * 
     * @param potreroId UUID del potrero
     * @return lista de UUIDs de animales actualmente en el potrero,
     *         lista vacía si el potrero no tiene animales
     */
    @Override
    @Transactional(readOnly = true)
    public List<UUID> findAnimalIdsByPotreroId(UUID potreroId) {
        return jpaRepository.findByPotreroIdAndFechaSalidaIsNull(potreroId)
                .stream()
                .map(PastureHistoryEntity::getAnimalId)
                .collect(Collectors.toList());
    }
    
    /**
     * Cuenta cuántos animales están actualmente en un potrero.
     * 
     * Retorna la cantidad de registros con:
     * - potrero_id = potreroId especificado
     * - fecha_salida = null (ubicación actual)
     * 
     * @param potreroId UUID del potrero
     * @return cantidad de animales con registro actual en el potrero
     */
    @Override
    @Transactional(readOnly = true)
    public int countAnimalsInPasture(UUID potreroId) {
        return (int) jpaRepository.countByPotreroIdAndFechaSalidaIsNull(potreroId);
    }
}
