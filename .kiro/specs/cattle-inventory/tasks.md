# Implementation Plan: Cattle Inventory Module

## Overview

Este plan de implementación detalla las tareas necesarias para construir el módulo `cattle-inventory` de Vacapp siguiendo la arquitectura Spring Modulith + Clean Architecture. El módulo proporciona gestión completa del inventario de ganado bovino con identificación única (arete), genealogía, trazabilidad de movimientos, historial de pesos, eventos de salud, y cumplimiento regulatorio mexicano (Folio REEMO).

Las tareas están organizadas en un grafo de dependencias para optimizar el desarrollo.

## Tasks

- [x] 1. Configuración inicial del módulo y dependencias
  - [x] 1.1. Crear estructura de paquetes: mx.vacapp.cattle/ con internal/
  - [x] 1.2. Actualizar pom.xml con dependencias: Spring Boot, Spring Modulith, JPA, validation
  - [x] 1.3. Configurar properties en application.yml para cattle module
  - [x] 1.4. Configurar plugin openapi-generator-maven-plugin para cattle API

- [x] 2. Crear entidades de dominio puras (domain layer)
  - [x] 2.1. Crear enum Sex.java (Macho, Hembra)
  - [x] 2.2. Crear enum Breed.java (Charolais, Angus, Brahman, Hereford, Simmental, Limousin, Criollo, Brangus, Santa Gertrudis, Cruzada)
  - [x] 2.3. Crear enum CattleStatus.java (Activa, Vendida, Muerta, Prestada, Preñada, En Reposo)
  - [x] 2.4. Crear enum CattleType.java (Venta, Cría, Engorda, Semental, Vientre)
  - [x] 2.5. Crear clase AgeCalculator.java con método calculateMonths()
  - [x] 2.6. Crear clase Animal.java como entidad de dominio inmutable con Builder pattern
  - [x] 2.7. Implementar métodos de negocio en Animal: isActive(), isSoldOrDead(), markAsSold(), markAsDead(), markAsPregnant()
  - [x] 2.8. Crear excepciones de dominio: DuplicateAreteException, InvalidGenealogyException, InvalidPastureException, SoldOrDeadAnimalException, AnimalNotFoundException

- [x] 3. Crear puertos de salida (repository interfaces)
  - [x] 3.1. Crear interfaz AnimalRepository.java con métodos: save, findById, findByArete, existsByArete, findAll, count, findByStatus, findByRancho
  - [x] 3.2. Crear interfaz PastureHistoryRepository.java con métodos: insert, findCurrentByAnimalId, updateFechaSalida, findHistoryByAnimalId
  - [x] 3.3. Crear interfaz WeightRepository.java con métodos: save, findByAnimalId, findPreviousWeight, findLatestWeight
  - [x] 3.4. Crear interfaz HealthEventRepository.java con métodos: save, findByAnimalId, findByEventType
  - [x] 3.5. Crear interfaz CattleAuditRepository.java con métodos: logAnimalCreation, logAnimalUpdate, logStatusChange, logMovement

- [x] 4. Crear entidades JPA y mappers
  - [x] 4.1. Crear AnimalEntity.java con anotaciones JPA completas
  - [x] 4.2. Crear PastureHistoryEntity.java con campo calculado dias_permanencia
  - [x] 4.3. Crear WeightEntity.java
  - [x] 4.4. Crear HealthEventEntity.java
  - [x] 4.5. Crear CattleAuditEntity.java
  - [x] 4.6. Crear AnimalMapper.java para mapear Animal ↔ AnimalEntity
  - [x] 4.7. Crear PastureHistoryMapper.java
  - [x] 4.8. Crear WeightMapper.java
  - [x] 4.9. Crear HealthEventMapper.java

- [x] 5. Implementar repositorios JPA
  - [x] 5.1. Crear AnimalJpaRepository extends JpaRepository<AnimalEntity, UUID>
  - [x] 5.2. Crear PastureHistoryJpaRepository extends JpaRepository<PastureHistoryEntity, UUID>
  - [x] 5.3. Crear WeightJpaRepository extends JpaRepository<WeightEntity, UUID>
  - [x] 5.4. Crear HealthEventJpaRepository extends JpaRepository<HealthEventEntity, UUID>
  - [x] 5.5. Crear CattleAuditJpaRepository extends JpaRepository<CattleAuditEntity, UUID>
  - [x] 5.6. Crear AnimalRepositoryImpl implements AnimalRepository con filtrado automático por tenant_id
  - [x] 5.7. Crear PastureHistoryRepositoryImpl implements PastureHistoryRepository
  - [x] 5.8. Crear WeightRepositoryImpl implements WeightRepository
  - [x] 5.9. Crear HealthEventRepositoryImpl implements HealthEventRepository
  - [x] 5.10. Crear CattleAuditRepositoryImpl implements CattleAuditRepository

- [x] 6. Implementar integración con módulo geográfico
  - [x] 6.1. Crear GeographyServiceClient.java para integración con geographic-control module
  - [x] 6.2. Implementar método validatePotreroActive(UUID potreroId) usando GeographyService.isPotreroActive()
  - [x] 6.3. Implementar manejo de errores cuando potrero no existe o está inactivo

- [x] 7. Crear comandos y resultados (application layer)
  - [x] 7.1. Crear CreateAnimalCommand.java con todos los campos del animal
  - [x] 7.2. Crear UpdateAnimalCommand.java
  - [x] 7.3. Crear AnimalResult.java con método fromDomain(Animal)
  - [x] 7.4. Crear MoveAnimalCommand.java con animal_id y potrero_destino_id
  - [x] 7.5. Crear RecordWeightCommand.java
  - [x] 7.6. Crear WeightResult.java con ganancia_diaria calculada
  - [x] 7.7. Crear RecordHealthEventCommand.java
  - [x] 7.8. Crear HealthEventResult.java

- [x] 8. Implementar casos de uso de animales
  - [x] 8.1. Crear CreateAnimalUseCase.java con validación de arete único, validación de potrero, creación de animal, inserción en pasture_history
  - [x] 8.2. Crear UpdateAnimalUseCase.java con validación de estado (no vendido/muerto)
  - [x] 8.3. Crear GetAnimalUseCase.java con cálculo de meses en tiempo real
  - [x] 8.4. Crear ListAnimalsUseCase.java con filtros por status, tipo, rancho, potrero
  - [x] 8.5. Crear ChangeStatusUseCase.java con validaciones específicas por status (Preñada solo hembras, Vendida con fecha/precio, Muerta con fecha/motivo)
  - [x] 8.6. Crear SearchAnimalsByAreteUseCase.java con búsqueda case-insensitive

- [x] 9. Implementar casos de uso de movimientos
  - [x] 9.1. Crear MoveAnimalUseCase.java con validación de animal activo, validación de potrero destino, actualización de fecha_salida en historial anterior, inserción de nuevo registro
  - [x] 9.2. Crear GetMovementHistoryUseCase.java con ordenación cronológica, cálculo de dias_permanencia
  - [x] 9.3. Crear ListAnimalsInPastureUseCase.java para obtener animales actuales en un potrero específico

- [x] 10. Implementar casos de uso de pesos
  - [x] 10.1. Crear RecordWeightUseCase.java con validación de animal activo, validación de fecha no futura
  - [x] 10.2. Crear GetWeightHistoryUseCase.java con ordenación por fecha
  - [x] 10.3. Crear CalculateDailyGainUseCase.java para calcular ganancia_diaria entre pesos consecutivos

- [x] 11. Implementar casos de uso de salud
  - [x] 11.1. Crear RecordHealthEventUseCase.java genérico para vaccination, treatment, diagnosis
  - [x] 11.2. Crear RecordBirthEventUseCase.java con creación automática de cría, validación de madre hembra, copiado de potrero madre, cambio de status madre si estaba Preñada
  - [x] 11.3. Crear GetHealthHistoryUseCase.java con ordenación cronológica
  - [x] 11.4. Crear GetUpcomingVaccinationsUseCase.java para calcular próximas vacunaciones basadas en intervalos

- [x] 12. Crear DTOs para API REST móvil
  - [x] 12.1. Crear CreateAnimalRequest.java con validaciones @NotNull, @Size, @Pattern para arete
  - [x] 12.2. Crear UpdateAnimalRequest.java
  - [x] 12.3. Crear AnimalResponse.java con campos: animal_id, arete, sexo, raza, edad_meses, tipo, status, potrero_actual, peso_actual, nombre_madre, nombre_padre
  - [x] 12.4. Crear AnimalDetailResponse.java extendiendo AnimalResponse con genealogía completa
  - [x] 12.5. Crear MoveAnimalRequest.java
  - [x] 12.6. Crear PastureHistoryResponse.java
  - [x] 12.7. Crear RecordWeightRequest.java con validación peso > 0
  - [x] 12.8. Crear WeightResponse.java con ganancia_diaria
  - [x] 12.9. Crear RecordHealthEventRequest.java con validación de event_type
  - [x] 12.10. Crear HealthEventResponse.java
  - [x] 12.11. Crear CattleStatsResponse.java
  - [x] 12.12. Crear ErrorResponse.java

- [ ] 13. Crear especificación OpenAPI YAML
  - [x] 13.1. Crear archivo src/main/resources/openapi/openapi-cattle.yaml
  - [x] 13.2. Definir schemas para todos los Request/Response DTOs
  - [-] 13.3. Definir endpoints principales: POST/GET/PUT/DELETE /api/v1/cattle, GET /api/v1/cattle/{id}/movements, POST /api/v1/cattle/{id}/move
  - [-] 13.4. Definir endpoints de pesos: GET/POST /api/v1/cattle/{id}/weights
  - [-] 13.5. Definir endpoints de salud: GET/POST /api/v1/cattle/{id}/health
  - [~] 13.6. Definir endpoints adicionales: GET /api/v1/cattle/{id}/offspring, GET /api/v1/pastures/{id}/cattle, GET /api/v1/cattle/stats
  - [~] 13.7. Especificar códigos HTTP por endpoint y configurar securitySchemes Bearer JWT

- [ ] 14. Implementar controladores REST para móvil
  - [~] 14.1. Crear AnimalRestController.java implements CattleApi con métodos CRUD
  - [~] 14.2. Crear MovementRestController.java con endpoints de movimientos
  - [~] 14.3. Crear WeightRestController.java con endpoints de pesos
  - [~] 14.4. Crear HealthRestController.java con endpoints de salud
  - [~] 14.5. Implementar mapeo de DTOs Request → Commands en controladores
  - [~] 14.6. Implementar mapeo de Results → DTOs Response en controladores
  - [~] 14.7. Implementar @ExceptionHandler para mapear excepciones de dominio a códigos HTTP

- [ ] 15. Crear DTOs y controladores web (Thymeleaf)
  - [~] 15.1. Crear AnimalFormDto.java para formulario de crear/editar animal
  - [~] 15.2. Crear MoveAnimalFormDto.java
  - [~] 15.3. Crear HealthEventFormDto.java
  - [~] 15.4. Crear CattleWebController.java con métodos: listAnimals(), showAnimalDetail(), showCreateForm()
  - [~] 15.5. Crear MovementWebController.java
  - [~] 15.6. Crear HealthWebController.java

- [ ] 16. Crear vistas Thymeleaf y assets CSS
  - [~] 16.1. Crear templates/cattle/list.html con tabla de animales y filtros por status/tipo
  - [~] 16.2. Crear templates/cattle/detail.html con tabs para Información, Genealogía, Movimientos, Pesos, Salud
  - [~] 16.3. Crear templates/cattle/create-form-modal.html
  - [~] 16.4. Crear templates/fragments/cattle-card.html
  - [~] 16.5. Crear templates/fragments/genealogy-tree.html con visualización de árbol familiar
  - [~] 16.6. Crear templates/fragments/health-timeline.html
  - [~] 16.7. Crear static/css/cattle.css con estilos para lista y detalle
  - [~] 16.8. Crear static/css/genealogy-tree.css para árbol genealógico visual
  - [~] 16.9. Implementar JavaScript vanilla para búsqueda en tiempo real de animales con debounce

- [ ] 17. Implementar API pública del módulo (CattleService)
  - [~] 17.1. Crear interfaz CattleService.java en raíz del paquete
  - [~] 17.2. Implementar CattleServiceImpl.java con métodos: isAnimalActive(UUID), getAnimalTenantId(UUID), countAnimalsInPasture(UUID), getAnimalSex(UUID)
  - [~] 17.3. Registrar bean CattleService en CattleModuleConfig.java

- [ ] 18. Crear scripts de migración de base de datos
  - [~] 18.1. Crear script V4__create_animals_table.sql con todos los campos, constraints, índices
  - [~] 18.2. Crear índice único en arete (global, no por tenant)
  - [~] 18.3. Crear script V5__create_pasture_history_table.sql con campo calculado dias_permanencia
  - [~] 18.4. Crear script V6__create_cattle_weights_table.sql
  - [~] 18.5. Crear script V7__create_health_events_table.sql
  - [~] 18.6. Crear script V8__create_cattle_audit_table.sql
  - [~] 18.7. Crear índices para queries comunes: tenant_id, rancho_id, status, arete, madre_id, padre_id

- [ ] 19. Implementar tests de integración para API REST
  - [~] 19.1. Configurar TestContainers con MySQL
  - [~] 19.2. Crear AnimalRestControllerIntegrationTest.java con tests CRUD completos
  - [~] 19.3. Crear MovementRestControllerIntegrationTest.java con tests de movimientos entre potreros
  - [~] 19.4. Crear WeightRestControllerIntegrationTest.java
  - [~] 19.5. Crear HealthRestControllerIntegrationTest.java con test especial de evento BIRTH
  - [~] 19.6. Verificar códigos HTTP correctos y filtrado multi-tenant
  - [~] 19.7. Verificar validación de arete único global
  - [~] 19.8. Verificar integración con GeographyService (mock)

- [ ] 20. Implementar tests unitarios de casos de uso
  - [~] 20.1. Crear CreateAnimalUseCaseTest.java con mocks
  - [~] 20.2. Crear MoveAnimalUseCaseTest.java
  - [~] 20.3. Crear RecordBirthEventUseCaseTest.java con verificación de creación automática de cría
  - [~] 20.4. Crear ChangeStatusUseCaseTest.java con validaciones específicas por status
  - [~] 20.5. Crear CalculateDailyGainUseCaseTest.java
  - [~] 20.6. Verificar que todos los casos de error lanzan excepción correcta

- [ ] 21. Implementar property-based tests con jqwik
  - [~] 21.1. Crear AreteUniquenessPropertiesTest.java para validar arete único global
  - [~] 21.2. Crear GenealogyValidationPropertiesTest.java para validar madre Hembra y padre Macho
  - [~] 21.3. Crear AgeCalculationPropertiesTest.java para validar cálculo de meses
  - [~] 21.4. Crear PastureLocationPropertiesTest.java para validar ubicación única (máximo 1 registro con fecha_salida null)
  - [~] 21.5. Crear ImmutableSoldDeadPropertiesTest.java para validar que animales Vendidos/Muertos no se pueden modificar

- [ ] 22. Configurar caching para estadísticas
  - [~] 22.1. Configurar @Cacheable en GetAnimalStatsUseCase con TTL 15 minutos
  - [~] 22.2. Configurar @CacheEvict en operaciones que modifican inventario
  - [~] 22.3. Crear CacheConfig.java con configuración Caffeine

- [ ] 23. Documentar módulo y generar JavaDoc
  - [~] 23.1. Añadir JavaDoc a CattleService.java
  - [~] 23.2. Añadir JavaDoc a todas las interfaces públicas de dominio
  - [~] 23.3. Crear README.md del módulo cattle-inventory
  - [~] 23.4. Generar JavaDoc con `mvn javadoc:javadoc`

- [ ] 24. Verificación final y smoke tests
  - [~] 24.1. Ejecutar `mvn clean install` sin errores
  - [~] 24.2. Iniciar aplicación y verificar Swagger UI
  - [~] 24.3. Probar flujo completo: crear animal, moverlo, registrar peso, registrar vacunación
  - [~] 24.4. Probar flujo de parto: crear madre preñada, registrar evento BIRTH, verificar cría creada automáticamente
  - [~] 24.5. Verificar interfaz web accesible
  - [~] 24.6. Ejecutar todos los tests con `mvn test` y verificar > 80% coverage
  - [~] 24.7. Verificar logs no contienen datos sensibles

## Task Dependency Graph

```json
{
  "waves": [
    {
      "name": "Setup",
      "tasks": [1]
    },
    {
      "name": "Foundation",
      "tasks": [2, 6, 18]
    },
    {
      "name": "Ports and Adapters",
      "tasks": [3, 4]
    },
    {
      "name": "Persistence",
      "tasks": [5]
    },
    {
      "name": "Application Layer",
      "tasks": [7]
    },
    {
      "name": "Use Cases",
      "tasks": [8, 9, 10, 11]
    },
    {
      "name": "API Contracts",
      "tasks": [12, 13, 15]
    },
    {
      "name": "Controllers",
      "tasks": [14, 16, 17]
    },
    {
      "name": "Testing",
      "tasks": [19, 20]
    },
    {
      "name": "Advanced Testing and Optimization",
      "tasks": [21, 22]
    },
    {
      "name": "Documentation",
      "tasks": [23]
    },
    {
      "name": "Verification",
      "tasks": [24]
    }
  ]
}
```

## Notes

- **Paralelización**: Las tareas 2, 6 y 18 pueden ejecutarse en paralelo después de la tarea 1
- **Integración crítica**: La tarea 6 (integración con geographic-control) es fundamental y debe probarse exhaustivamente
- **Arete único global**: La validación de arete debe ser a nivel global (no por tenant) - implementar índice único sin tenant_id
- **Evento BIRTH**: La tarea 11.2 es compleja ya que crea automáticamente un animal nuevo - implementar en transacción atómica
- **Property-based tests**: La tarea 21 valida propiedades críticas que garantizan la integridad del inventario
- **Campo meses calculado**: Usar columna generada (GENERATED ALWAYS AS) en MySQL para cálculo automático
- **Genealogía**: Validar siempre que madre_id sea Hembra y padre_id sea Macho antes de persistir
- **Historial de movimientos**: Garantizar que cada animal tenga máximo 1 registro con fecha_salida = null
- **Multi-tenancy**: Aplicar filtrado por tenant_id en todas las operaciones excepto validación de arete único
- **OpenAPI Design-First**: La tarea 13 define el contrato que guía la implementación de la tarea 14
