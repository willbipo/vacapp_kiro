# Implementation Plan: Vaccination Management Module

## Overview

Este plan detalla las tareas para construir el módulo `vaccination-management` de Vacapp siguiendo Spring Modulith + Clean Architecture. El módulo proporciona gestión completa del inventario de vacunas, aplicación a ganado con descuento automático de stock, cálculo inteligente de próximas dosis, alertas de stock bajo y vencimientos, y reportes de consumo y costos.

## Tasks

- [ ] 1. Configuración inicial del módulo
  - [ ] 1.1. Crear estructura de paquetes: mx.vacapp.vaccination/ con internal/
  - [ ] 1.2. Actualizar pom.xml con dependencias necesarias
  - [ ] 1.3. Configurar properties en application.yml
  - [ ] 1.4. Configurar openapi-generator-maven-plugin para vaccination API

- [ ] 2. Crear entidades de dominio puras
  - [ ] 2.1. Crear clase VaccineCategory.java (entidad o enum)
  - [ ] 2.2. Crear clase VaccineType.java (entidad o enum)
  - [ ] 2.3. Crear clase AdministrationRoute.java (entidad o enum)
  - [ ] 2.4. Crear clase Vaccine.java con métodos requiresBooster(), isLowStock()
  - [ ] 2.5. Crear clase VaccineLot.java con métodos isExpired(), hasStock(), decrementStock(), daysUntilExpiration()
  - [ ] 2.6. Crear clase VaccinationApplication.java con métodos hasNextDose(), isNextDoseDue(), daysUntilNextDose()
  - [ ] 2.7. Crear clase NextDoseCalculator.java con método estático calculate()
  - [ ] 2.8. Crear clase StockAlert.java
  - [ ] 2.9. Crear clase ExpirationAlert.java
  - [ ] 2.10. Crear excepciones: VaccineNotFoundException, InsufficientStockException, ExpiredVaccineException, InvalidVeterinarianException, InvalidAnimalException

- [ ] 3. Crear puertos de salida (repository interfaces)
  - [ ] 3.1. Crear interfaz VaccineRepository.java con métodos CRUD y filtros
  - [ ] 3.2. Crear interfaz VaccineLotRepository.java con métodos para gestión de lotes
  - [ ] 3.3. Crear interfaz VaccinationApplicationRepository.java
  - [ ] 3.4. Crear interfaz StockAlertRepository.java
  - [ ] 3.5. Crear interfaz ExpirationAlertRepository.java
  - [ ] 3.6. Crear interfaz VaccinationAuditRepository.java

- [ ] 4. Crear entidades JPA y mappers
  - [ ] 4.1. Crear VaccineCategoryEntity.java con datos pre-cargados (5 categorías)
  - [ ] 4.2. Crear VaccineTypeEntity.java con datos pre-cargados (5 tipos)
  - [ ] 4.3. Crear AdministrationRouteEntity.java con datos pre-cargados (5 vías)
  - [ ] 4.4. Crear VaccineEntity.java con todas las relaciones FK
  - [ ] 4.5. Crear VaccineLotEntity.java
  - [ ] 4.6. Crear VaccinationApplicationEntity.java
  - [ ] 4.7. Crear StockAlertEntity.java
  - [ ] 4.8. Crear ExpirationAlertEntity.java
  - [ ] 4.9. Crear StockAdjustmentEntity.java
  - [ ] 4.10. Crear VaccinationAuditEntity.java
  - [ ] 4.11. Crear VaccineMapper.java para mapeo bidireccional
  - [ ] 4.12. Crear VaccineLotMapper.java
  - [ ] 4.13. Crear VaccinationApplicationMapper.java

- [ ] 5. Implementar repositorios JPA
  - [ ] 5.1. Crear VaccineJpaRepository extends JpaRepository<VaccineEntity, UUID>
  - [ ] 5.2. Crear VaccineLotJpaRepository con custom queries para lotes por vencer
  - [ ] 5.3. Crear VaccinationApplicationJpaRepository con queries complejas
  - [ ] 5.4. Crear StockAlertJpaRepository
  - [ ] 5.5. Crear ExpirationAlertJpaRepository
  - [ ] 5.6. Crear VaccineRepositoryImpl con filtrado por tenant_id y rancho_id
  - [ ] 5.7. Crear VaccineLotRepositoryImpl
  - [ ] 5.8. Crear VaccinationApplicationRepositoryImpl
  - [ ] 5.9. Crear StockAlertRepositoryImpl
  - [ ] 5.10. Crear ExpirationAlertRepositoryImpl

- [ ] 6. Implementar integración con otros módulos
  - [ ] 6.1. Crear CattleServiceClient.java para integración con cattle-inventory
  - [ ] 6.2. Implementar método validateAnimalActive(UUID animalId)
  - [ ] 6.3. Implementar método getAnimalInfo(UUID animalId) para obtener arete
  - [ ] 6.4. Crear UserServiceClient.java para integración con user-management
  - [ ] 6.5. Implementar método validateVeterinarian(UUID userId)

- [ ] 7. Crear comandos y resultados
  - [ ] 7.1. Crear CreateVaccineCommand.java
  - [ ] 7.2. Crear VaccineResult.java
  - [ ] 7.3. Crear CreateVaccineLotCommand.java
  - [ ] 7.4. Crear VaccineLotResult.java
  - [ ] 7.5. Crear ApplyVaccinationCommand.java
  - [ ] 7.6. Crear VaccinationApplicationResult.java con proxima_dosis_fecha
  - [ ] 7.7. Crear ConsumptionReportResult.java
  - [ ] 7.8. Crear CostReportResult.java

- [ ] 8. Implementar casos de uso de vacunas
  - [ ] 8.1. Crear CreateVaccineUseCase.java con validación de categoría, tipo, vía
  - [ ] 8.2. Crear UpdateVaccineUseCase.java
  - [ ] 8.3. Crear GetVaccineUseCase.java con cálculo de stock_total
  - [ ] 8.4. Crear ListVaccinesUseCase.java con filtros por categoría, activo
  - [ ] 8.5. Crear ArchiveVaccineUseCase.java (soft delete)

- [ ] 9. Implementar casos de uso de lotes
  - [ ] 9.1. Crear CreateVaccineLotUseCase.java con validación de fecha_vencimiento no pasada
  - [ ] 9.2. Crear AdjustStockUseCase.java con registro en stock_adjustments
  - [ ] 9.3. Crear GetLotsByVaccineUseCase.java ordenado por fecha_vencimiento
  - [ ] 9.4. Crear MarkExpiredLotsUseCase.java para marcar lotes vencidos (job)

- [ ] 10. Implementar casos de uso de aplicación de vacunas
  - [ ] 10.1. Crear ApplyVaccinationUseCase.java con lógica completa: validar animal activo, validar stock > 0, validar lote no vencido, validar veterinario, decrementar stock (transaccional), calcular próxima dosis, crear alerta si stock <= minimo
  - [ ] 10.2. Crear GetVaccinationHistoryUseCase.java para historial por animal
  - [ ] 10.3. Crear GetUpcomingVaccinationsUseCase.java para animales con vacunas pendientes
  - [ ] 10.4. Crear GetUnvaccinatedAnimalsUseCase.java para animales sin vacuna específica

- [ ] 11. Implementar casos de uso de alertas
  - [ ] 11.1. Crear CheckStockAlertsUseCase.java (ejecutado por scheduler)
  - [ ] 11.2. Crear CheckExpirationAlertsUseCase.java (ejecutado por scheduler)
  - [ ] 11.3. Crear GetActiveStockAlertsUseCase.java
  - [ ] 11.4. Crear GetActiveExpirationAlertsUseCase.java con clasificación por nivel (CRITICO, IMPORTANTE, INFORMATIVO)

- [ ] 12. Implementar casos de uso de reportes
  - [ ] 12.1. Crear GenerateConsumptionReportUseCase.java con agrupación por vacuna, periodo
  - [ ] 12.2. Crear GenerateCostReportUseCase.java con cálculos de costo total, promedio, por categoría
  - [ ] 12.3. Crear CalculateCoverageUseCase.java para calcular porcentaje de cobertura

- [ ] 13. Crear DTOs para API REST
  - [ ] 13.1. Crear CreateVaccineRequest.java con validaciones @NotNull, @Size
  - [ ] 13.2. Crear VaccineResponse.java
  - [ ] 13.3. Crear VaccineDetailResponse.java con stock_total, lotes_activos
  - [ ] 13.4. Crear CreateVaccineLotRequest.java
  - [ ] 13.5. Crear VaccineLotResponse.java con dias_para_vencer calculado
  - [ ] 13.6. Crear ApplyVaccinationRequest.java
  - [ ] 13.7. Crear VaccinationApplicationResponse.java
  - [ ] 13.8. Crear VaccinationHistoryResponse.java
  - [ ] 13.9. Crear StockAlertResponse.java
  - [ ] 13.10. Crear ExpirationAlertResponse.java
  - [ ] 13.11. Crear ConsumptionReportResponse.java
  - [ ] 13.12. Crear CostReportResponse.java

- [ ] 14. Crear especificación OpenAPI YAML
  - [ ] 14.1. Crear archivo src/main/resources/openapi/openapi-vaccination.yaml
  - [ ] 14.2. Definir schemas para todos los DTOs
  - [ ] 14.3. Definir endpoints de vacunas: POST/GET/PUT/DELETE /api/v1/vaccines
  - [ ] 14.4. Definir endpoints de lotes: GET /api/v1/vaccines/{id}/lots, POST /api/v1/vaccine-lots, PUT /api/v1/vaccine-lots/{id}/adjust-stock
  - [ ] 14.5. Definir endpoints de aplicaciones: POST /api/v1/vaccinations, GET /api/v1/animals/{id}/vaccinations
  - [ ] 14.6. Definir endpoints de alertas: GET /api/v1/alerts/stock, GET /api/v1/alerts/expiration
  - [ ] 14.7. Definir endpoints de reportes: GET /api/v1/reports/vaccine-consumption, GET /api/v1/reports/vaccination-costs
  - [ ] 14.8. Definir endpoints de catálogos: GET /api/v1/vaccines/categories, GET /api/v1/vaccines/types, GET /api/v1/vaccines/routes
  - [ ] 14.9. Configurar securitySchemes Bearer JWT

- [ ] 15. Implementar controladores REST
  - [ ] 15.1. Crear VaccineRestController.java con CRUD de vacunas
  - [ ] 15.2. Crear VaccineLotRestController.java
  - [ ] 15.3. Crear VaccinationRestController.java con aplicación de vacunas
  - [ ] 15.4. Crear AlertRestController.java
  - [ ] 15.5. Crear ReportRestController.java
  - [ ] 15.6. Implementar mapeo de DTOs en controladores
  - [ ] 15.7. Implementar @ExceptionHandler para excepciones de dominio

- [ ] 16. Crear DTOs y controladores web (Thymeleaf)
  - [ ] 16.1. Crear VaccineFormDto.java
  - [ ] 16.2. Crear ApplyVaccinationFormDto.java con autocomplete de animales
  - [ ] 16.3. Crear VaccineWebController.java con métodos listVaccines(), showVaccineDetail(), showCreateForm()
  - [ ] 16.4. Crear VaccinationWebController.java

- [ ] 17. Crear vistas Thymeleaf y assets CSS
  - [ ] 17.1. Crear templates/vaccines/list.html con tabla de vacunas
  - [ ] 17.2. Crear templates/vaccines/detail.html con tabs: Info, Lotes, Aplicaciones, Stats
  - [ ] 17.3. Crear templates/vaccines/create-form-modal.html
  - [ ] 17.4. Crear templates/vaccinations/apply-form.html con selects dinámicos
  - [ ] 17.5. Crear templates/vaccinations/history.html
  - [ ] 17.6. Crear templates/alerts/dashboard.html con cards de alertas
  - [ ] 17.7. Crear templates/reports/consumption.html
  - [ ] 17.8. Crear templates/reports/costs.html
  - [ ] 17.9. Crear static/css/vaccines.css
  - [ ] 17.10. Implementar JavaScript vanilla para autocomplete de animales

- [ ] 18. Implementar schedulers para alertas
  - [ ] 18.1. Crear StockAlertScheduler.java con @Scheduled(cron = "0 0 8 * * *")
  - [ ] 18.2. Implementar lógica: calcular stock total por vacuna, crear/resolver alertas
  - [ ] 18.3. Crear ExpirationAlertScheduler.java con @Scheduled(cron = "0 0 6 * * *")
  - [ ] 18.4. Implementar lógica: buscar lotes por vencer en 30 días, crear alertas, marcar lotes vencidos
  - [ ] 18.5. Crear SchedulingConfig.java para habilitar scheduling

- [ ] 19. Implementar API pública del módulo
  - [ ] 19.1. Crear interfaz VaccinationService.java en raíz
  - [ ] 19.2. Implementar VaccinationServiceImpl.java con métodos: getVaccinationCountByAnimal(), getNextScheduledVaccination(), isVaccineActive()
  - [ ] 19.3. Registrar bean en VaccinationModuleConfig.java

- [ ] 20. Crear scripts de migración de base de datos
  - [ ] 20.1. Crear script V9__create_vaccine_categories_table.sql con INSERT de 5 categorías
  - [ ] 20.2. Crear script V10__create_vaccine_types_table.sql con INSERT de 5 tipos
  - [ ] 20.3. Crear script V11__create_administration_routes_table.sql con INSERT de 5 vías
  - [ ] 20.4. Crear script V12__create_vaccines_table.sql con FKs y constraints
  - [ ] 20.5. Crear script V13__create_vaccine_lots_table.sql
  - [ ] 20.6. Crear script V14__create_vaccination_applications_table.sql
  - [ ] 20.7. Crear script V15__create_stock_alerts_table.sql
  - [ ] 20.8. Crear script V16__create_expiration_alerts_table.sql
  - [ ] 20.9. Crear script V17__create_stock_adjustments_table.sql
  - [ ] 20.10. Crear script V18__create_vaccination_audit_table.sql
  - [ ] 20.11. Crear índices para optimizar queries: tenant_id+rancho_id, fecha_aplicacion, proxima_dosis_fecha

- [ ] 21. Implementar tests de integración
  - [ ] 21.1. Configurar TestContainers con MySQL
  - [ ] 21.2. Crear ApplyVaccinationIntegrationTest.java con tests completos: aplicación exitosa, stock insuficiente, lote vencido, animal inactivo, veterinario inválido
  - [ ] 21.3. Verificar descuento de stock es transaccional (test con rollback)
  - [ ] 21.4. Verificar cálculo correcto de próxima dosis
  - [ ] 21.5. Verificar creación automática de alerta cuando stock <= minimo
  - [ ] 21.6. Crear VaccineRestControllerIntegrationTest.java
  - [ ] 21.7. Crear ReportIntegrationTest.java

- [ ] 22. Implementar tests unitarios
  - [ ] 22.1. Crear CreateVaccineUseCaseTest.java con mocks
  - [ ] 22.2. Crear ApplyVaccinationUseCaseTest.java con mocks
  - [ ] 22.3. Crear NextDoseCalculatorTest.java para validar cálculo
  - [ ] 22.4. Crear VaccineLotTest.java para validar decrementStock(), isExpired()
  - [ ] 22.5. Crear CheckStockAlertsUseCaseTest.java
  - [ ] 22.6. Crear GenerateConsumptionReportUseCaseTest.java

- [ ] 23. Implementar tests de schedulers
  - [ ] 23.1. Crear StockAlertSchedulerTest.java con Spring Boot Test
  - [ ] 23.2. Crear ExpirationAlertSchedulerTest.java
  - [ ] 23.3. Verificar que jobs se ejecutan en horarios correctos

- [ ] 24. Implementar tests de concurrencia
  - [ ] 24.1. Crear ConcurrentVaccinationTest.java que simula múltiples threads aplicando la misma vacuna
  - [ ] 24.2. Verificar que stock final es correcto (sin overselling)
  - [ ] 24.3. Verificar que transacciones previenen condiciones de carrera

- [ ] 25. Configurar caching
  - [ ] 25.1. Configurar @Cacheable en GetVaccineUseCase para vacunas frecuentemente consultadas
  - [ ] 25.2. Configurar @CacheEvict al actualizar/archivar vacunas
  - [ ] 25.3. Configurar cache para reportes con TTL 1 hora

- [ ] 26. Documentar módulo
  - [ ] 26.1. Añadir JavaDoc a VaccinationService.java
  - [ ] 26.2. Crear README.md del módulo con ejemplos de uso
  - [ ] 26.3. Generar JavaDoc con `mvn javadoc:javadoc`

- [ ] 27. Verificación final
  - [ ] 27.1. Ejecutar `mvn clean install` sin errores
  - [ ] 27.2. Verificar Swagger UI con todos los endpoints
  - [ ] 27.3. Probar flujo completo: crear vacuna, crear lote, aplicar a animal, verificar stock decrementado, verificar próxima dosis calculada
  - [ ] 27.4. Probar alertas: simular stock bajo, verificar alerta creada
  - [ ] 27.5. Probar schedulers manualmente cambiando fechas
  - [ ] 27.6. Ejecutar todos los tests con `mvn test` y verificar > 80% coverage
  - [ ] 27.7. Verificar multi-tenancy: usuario de tenant A no puede ver vacunas de tenant B

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
      "tasks": [2, 20]
    },
    {
      "name": "Ports",
      "tasks": [3]
    },
    {
      "name": "Persistence",
      "tasks": [4, 5]
    },
    {
      "name": "Integration",
      "tasks": [6]
    },
    {
      "name": "Application Layer",
      "tasks": [7]
    },
    {
      "name": "Use Cases",
      "tasks": [8, 9, 10, 11, 12]
    },
    {
      "name": "API Contracts",
      "tasks": [13, 14, 16]
    },
    {
      "name": "Controllers",
      "tasks": [15, 17, 19]
    },
    {
      "name": "Schedulers",
      "tasks": [18]
    },
    {
      "name": "Testing",
      "tasks": [21, 22, 23, 24]
    },
    {
      "name": "Optimization",
      "tasks": [25]
    },
    {
      "name": "Documentation",
      "tasks": [26]
    },
    {
      "name": "Verification",
      "tasks": [27]
    }
  ]
}
```

## Notes

- **Paralelización**: Tareas 2 y 20 pueden ejecutarse en paralelo después de tarea 1
- **Transaccionalidad crítica**: La tarea 10.1 (ApplyVaccinationUseCase) DEBE ser transaccional para evitar inconsistencias de stock
- **Integración con cattle-inventory**: La tarea 6 es fundamental - siempre validar estado del animal antes de vacunar
- **Cálculo de próxima dosis**: El NextDoseCalculator debe probarse exhaustivamente (tarea 22.3)
- **Schedulers**: Deben ejecutarse en horarios de baja carga (6 AM y 8 AM)
- **Alertas**: Críticas para evitar falta de stock y uso de vacunas vencidas
- **Tests de concurrencia**: La tarea 24 valida que no haya overselling de stock
- **Multi-tenancy**: Aplicar filtrado en todas las operaciones
- **OpenAPI Design-First**: Tarea 14 define contrato que guía implementación de tarea 15
- **Reportes**: Considerar caché agresivo ya que son queries costosas
- **Datos de catálogos**: Insertar categorías, tipos y vías en scripts de migración (no hardcodear en código)
