# Implementation Plan: Inventory Management Module

## Overview

Este plan detalla las tareas para construir el módulo `inventory-management` de Vacapp siguiendo Spring Modulith + Clean Architecture. El módulo proporciona gestión completa del inventario de insumos con categorías personalizadas, movimientos rastreables, alertas automáticas y reportes de valoración y costos.

## Tasks

- [ ] 1. Configuración inicial del módulo
  - [ ] 1.1. Crear estructura de paquetes: mx.vacapp.inventory/ con internal/
  - [ ] 1.2. Actualizar pom.xml con dependencias
  - [ ] 1.3. Configurar properties en application.yml
  - [ ] 1.4. Configurar openapi-generator-maven-plugin

- [ ] 2. Crear entidades de dominio puras
  - [ ] 2.1. Crear enum MovementType (ENTRADA, SALIDA)
  - [ ] 2.2. Crear enum MovementReason (COMPRA, CONSUMO, MERMA, DONACION, AJUSTE_ENTRADA, AJUSTE_SALIDA, DEVOLUCION) con métodos isEntryReason(), isExitReason()
  - [ ] 2.3. Crear enum StockStatus (OK, BAJO, CRITICO)
  - [ ] 2.4. Crear clase MeasurementUnit.java
  - [ ] 2.5. Crear clase SupplyCategory.java
  - [ ] 2.6. Crear clase Supply.java con métodos: getStockStatus(), isExpired(), hasStock(), incrementStock(), decrementStock(), getValorTotal(), updatePrecioPromedioPonderado()
  - [ ] 2.7. Crear clase SupplyMovement.java
  - [ ] 2.8. Crear clase PastureConsumption.java
  - [ ] 2.9. Crear clase StockAlert.java
  - [ ] 2.10. Crear clase ExpirationAlert.java
  - [ ] 2.11. Crear excepciones: SupplyNotFoundException, InsufficientStockException, CategoryNotFoundException, InvalidMovementException, ExpiredSupplyException

- [ ] 3. Crear puertos de salida (repository interfaces)
  - [ ] 3.1. Crear interfaz SupplyRepository.java
  - [ ] 3.2. Crear interfaz SupplyCategoryRepository.java
  - [ ] 3.3. Crear interfaz SupplyMovementRepository.java
  - [ ] 3.4. Crear interfaz StockAlertRepository.java
  - [ ] 3.5. Crear interfaz ExpirationAlertRepository.java
  - [ ] 3.6. Crear interfaz PastureConsumptionRepository.java
  - [ ] 3.7. Crear interfaz InventoryAuditRepository.java

- [ ] 4. Crear entidades JPA y mappers
  - [ ] 4.1. Crear MeasurementUnitEntity.java con datos pre-cargados (14 unidades)
  - [ ] 4.2. Crear SupplyCategoryEntity.java
  - [ ] 4.3. Crear SupplyEntity.java con todas las relaciones FK
  - [ ] 4.4. Crear SupplyMovementEntity.java
  - [ ] 4.5. Crear PastureConsumptionEntity.java
  - [ ] 4.6. Crear StockAlertEntity.java
  - [ ] 4.7. Crear ExpirationAlertEntity.java
  - [ ] 4.8. Crear InventoryAuditEntity.java
  - [ ] 4.9. Crear SupplyMapper.java
  - [ ] 4.10. Crear SupplyCategoryMapper.java
  - [ ] 4.11. Crear SupplyMovementMapper.java

- [ ] 5. Implementar repositorios JPA
  - [ ] 5.1. Crear SupplyJpaRepository extends JpaRepository
  - [ ] 5.2. Crear SupplyCategoryJpaRepository
  - [ ] 5.3. Crear SupplyMovementJpaRepository
  - [ ] 5.4. Crear StockAlertJpaRepository
  - [ ] 5.5. Crear ExpirationAlertJpaRepository
  - [ ] 5.6. Crear SupplyRepositoryImpl con filtrado por tenant_id y rancho_id
  - [ ] 5.7. Crear SupplyCategoryRepositoryImpl
  - [ ] 5.8. Crear SupplyMovementRepositoryImpl
  - [ ] 5.9. Crear StockAlertRepositoryImpl
  - [ ] 5.10. Crear ExpirationAlertRepositoryImpl

- [ ] 6. Implementar integración con otros módulos
  - [ ] 6.1. Crear GeographyServiceClient.java
  - [ ] 6.2. Implementar método validatePotreroActive(UUID potreroId)
  - [ ] 6.3. Crear UserServiceClient.java
  - [ ] 6.4. Implementar método validateUser(UUID userId)

- [ ] 7. Crear comandos y resultados
  - [ ] 7.1. Crear CreateCategoryCommand.java
  - [ ] 7.2. Crear CategoryResult.java
  - [ ] 7.3. Crear CreateSupplyCommand.java
  - [ ] 7.4. Crear SupplyResult.java
  - [ ] 7.5. Crear RegisterMovementCommand.java
  - [ ] 7.6. Crear MovementResult.java
  - [ ] 7.7. Crear ValuationReportResult.java
  - [ ] 7.8. Crear ConsumptionReportResult.java

- [ ] 8. Implementar casos de uso de categorías
  - [ ] 8.1. Crear CreateCategoryUseCase.java con validación de nombre único por rancho
  - [ ] 8.2. Crear UpdateCategoryUseCase.java
  - [ ] 8.3. Crear GetCategoryUseCase.java con cálculo de total_insumos y valor_total
  - [ ] 8.4. Crear ListCategoriesUseCase.java
  - [ ] 8.5. Crear ArchiveCategoryUseCase.java con validación de que no tenga insumos activos

- [ ] 9. Implementar casos de uso de insumos
  - [ ] 9.1. Crear CreateSupplyUseCase.java con validación de categoría y unidad
  - [ ] 9.2. Crear UpdateSupplyUseCase.java (sin permitir modificar cantidad directamente)
  - [ ] 9.3. Crear GetSupplyUseCase.java con cálculo de stock_status y dias_para_vencer
  - [ ] 9.4. Crear ListSuppliesUseCase.java con filtros por categoría, stock_bajo
  - [ ] 9.5. Crear SearchSuppliesUseCase.java con LIKE en nombre, descripcion, proveedor, ubicacion
  - [ ] 9.6. Crear ArchiveSupplyUseCase.java

- [ ] 10. Implementar casos de uso de movimientos
  - [ ] 10.1. Crear RegisterStockInUseCase.java con lógica completa: incrementar stock transaccional, actualizar precio promedio ponderado si se proporciona precio, resolver alertas de stock bajo si stock > minimo
  - [ ] 10.2. Crear RegisterStockOutUseCase.java con lógica: validar stock suficiente, decrementar transaccional, validar potrero si motivo = CONSUMO, crear alerta si stock <= minimo, calcular costo_calculado, registrar en pasture_consumption si aplica
  - [ ] 10.3. Crear GetMovementHistoryUseCase.java
  - [ ] 10.4. Crear GetMovementsByPeriodUseCase.java

- [ ] 11. Implementar casos de uso de alertas
  - [ ] 11.1. Crear CheckStockAlertsUseCase.java (ejecutado por scheduler)
  - [ ] 11.2. Crear CheckExpirationAlertsUseCase.java (ejecutado por scheduler)
  - [ ] 11.3. Crear GetActiveStockAlertsUseCase.java
  - [ ] 11.4. Crear GetActiveExpirationAlertsUseCase.java

- [ ] 12. Implementar casos de uso de reportes
  - [ ] 12.1. Crear GenerateInventoryValuationUseCase.java con cálculo de valor total, agrupación por categoría, top 10 más valiosos
  - [ ] 12.2. Crear GenerateConsumptionReportUseCase.java con agrupación por insumo, filtros por categoría/potrero/periodo
  - [ ] 12.3. Crear GenerateCostReportUseCase.java con cálculos de costo total, agrupación por categoría/motivo/potrero
  - [ ] 12.4. Crear GetConsumptionByPastureUseCase.java

- [ ] 13. Crear DTOs para API REST
  - [ ] 13.1. Crear CreateCategoryRequest.java con validaciones @NotNull, @Size
  - [ ] 13.2. Crear CategoryResponse.java
  - [ ] 13.3. Crear CreateSupplyRequest.java
  - [ ] 13.4. Crear SupplyResponse.java
  - [ ] 13.5. Crear SupplyDetailResponse.java con stock_status, dias_para_vencer
  - [ ] 13.6. Crear RegisterMovementRequest.java
  - [ ] 13.7. Crear MovementResponse.java
  - [ ] 13.8. Crear StockAlertResponse.java
  - [ ] 13.9. Crear ExpirationAlertResponse.java
  - [ ] 13.10. Crear ValuationReportResponse.java
  - [ ] 13.11. Crear ConsumptionReportResponse.java

- [ ] 14. Crear especificación OpenAPI YAML
  - [ ] 14.1. Crear archivo src/main/resources/openapi/openapi-inventory.yaml
  - [ ] 14.2. Definir schemas para todos los DTOs
  - [ ] 14.3. Definir endpoints de categorías: POST/GET/PUT/DELETE /api/v1/inventory/categories
  - [ ] 14.4. Definir endpoints de insumos: POST/GET/PUT/DELETE /api/v1/inventory/supplies
  - [ ] 14.5. Definir endpoints de movimientos: POST /api/v1/inventory/movements, GET /api/v1/inventory/supplies/{id}/movements
  - [ ] 14.6. Definir endpoints de alertas: GET /api/v1/inventory/alerts/stock, GET /api/v1/inventory/alerts/expiration
  - [ ] 14.7. Definir endpoints de reportes: GET /api/v1/reports/inventory-valuation, GET /api/v1/reports/supply-consumption, GET /api/v1/reports/consumption-costs
  - [ ] 14.8. Definir endpoint de unidades: GET /api/v1/inventory/units
  - [ ] 14.9. Configurar securitySchemes Bearer JWT

- [ ] 15. Implementar controladores REST
  - [ ] 15.1. Crear SupplyCategoryRestController.java
  - [ ] 15.2. Crear SupplyRestController.java
  - [ ] 15.3. Crear MovementRestController.java
  - [ ] 15.4. Crear AlertRestController.java
  - [ ] 15.5. Crear ReportRestController.java
  - [ ] 15.6. Implementar mapeo de DTOs
  - [ ] 15.7. Implementar @ExceptionHandler

- [ ] 16. Crear DTOs y controladores web (Thymeleaf)
  - [ ] 16.1. Crear SupplyFormDto.java
  - [ ] 16.2. Crear MovementFormDto.java
  - [ ] 16.3. Crear InventoryWebController.java
  - [ ] 16.4. Crear MovementWebController.java

- [ ] 17. Crear vistas Thymeleaf y assets CSS
  - [ ] 17.1. Crear templates/inventory/supplies/list.html
  - [ ] 17.2. Crear templates/inventory/supplies/detail.html con tabs
  - [ ] 17.3. Crear templates/inventory/supplies/create-form-modal.html
  - [ ] 17.4. Crear templates/inventory/movements/register-form.html
  - [ ] 17.5. Crear templates/inventory/categories/list.html
  - [ ] 17.6. Crear templates/inventory/alerts/dashboard.html
  - [ ] 17.7. Crear templates/inventory/reports/valuation.html
  - [ ] 17.8. Crear templates/inventory/reports/consumption.html
  - [ ] 17.9. Crear static/css/inventory.css
  - [ ] 17.10. Implementar JavaScript vanilla para búsqueda en tiempo real

- [ ] 18. Implementar schedulers para alertas
  - [ ] 18.1. Crear StockAlertScheduler.java con @Scheduled(cron = "0 0 8 * * *")
  - [ ] 18.2. Implementar lógica: verificar stock <= cantidad_minima, crear/resolver alertas
  - [ ] 18.3. Crear ExpirationAlertScheduler.java con @Scheduled(cron = "0 0 7 * * *")
  - [ ] 18.4. Implementar lógica: buscar insumos por vencer en 30 días, clasificar por urgencia, marcar vencidos
  - [ ] 18.5. Crear SchedulingConfig.java

- [ ] 19. Implementar API pública del módulo
  - [ ] 19.1. Crear interfaz InventoryService.java
  - [ ] 19.2. Implementar InventoryServiceImpl.java
  - [ ] 19.3. Registrar bean en InventoryModuleConfig.java

- [ ] 20. Crear scripts de migración de base de datos
  - [ ] 20.1. Crear script V19__create_measurement_units_table.sql con INSERT de 14 unidades
  - [ ] 20.2. Crear script V20__create_supply_categories_table.sql
  - [ ] 20.3. Crear script V21__create_supplies_table.sql con FKs y constraints
  - [ ] 20.4. Crear script V22__create_supply_movements_table.sql
  - [ ] 20.5. Crear script V23__create_pasture_supply_consumption_table.sql
  - [ ] 20.6. Crear script V24__create_stock_alerts_table.sql
  - [ ] 20.7. Crear script V25__create_expiration_alerts_table.sql
  - [ ] 20.8. Crear script V26__create_inventory_audit_table.sql
  - [ ] 20.9. Crear índices optimizados

- [ ] 21. Implementar tests de integración
  - [ ] 21.1. Configurar TestContainers
  - [ ] 21.2. Crear RegisterStockInIntegrationTest.java con tests: entrada exitosa, actualización de precio promedio ponderado, resolución de alerta de stock
  - [ ] 21.3. Crear RegisterStockOutIntegrationTest.java con tests: salida exitosa, stock insuficiente, creación de alerta, consumo vinculado a potrero
  - [ ] 21.4. Verificar transaccionalidad de movimientos
  - [ ] 21.5. Crear CategoryRestControllerIntegrationTest.java
  - [ ] 21.6. Crear ReportIntegrationTest.java

- [ ] 22. Implementar tests unitarios
  - [ ] 22.1. Crear CreateSupplyUseCaseTest.java
  - [ ] 22.2. Crear RegisterStockInUseCaseTest.java con mock
  - [ ] 22.3. Crear RegisterStockOutUseCaseTest.java
  - [ ] 22.4. Crear SupplyTest.java para validar updatePrecioPromedioPonderado(), getStockStatus()
  - [ ] 22.5. Crear GenerateInventoryValuationUseCaseTest.java

- [ ] 23. Implementar tests de schedulers
  - [ ] 23.1. Crear StockAlertSchedulerTest.java
  - [ ] 23.2. Crear ExpirationAlertSchedulerTest.java

- [ ] 24. Implementar tests de concurrencia
  - [ ] 24.1. Crear ConcurrentMovementsTest.java que simula múltiples threads registrando movimientos
  - [ ] 24.2. Verificar que stock final es correcto
  - [ ] 24.3. Verificar que transacciones previenen condiciones de carrera

- [ ] 25. Configurar caching
  - [ ] 25.1. Configurar @Cacheable en GetSupplyUseCase
  - [ ] 25.2. Configurar @CacheEvict al actualizar/archivar
  - [ ] 25.3. Configurar cache para reportes con TTL 1 hora

- [ ] 26. Documentar módulo
  - [ ] 26.1. Añadir JavaDoc a InventoryService.java
  - [ ] 26.2. Crear README.md del módulo
  - [ ] 26.3. Generar JavaDoc

- [ ] 27. Verificación final
  - [ ] 27.1. Ejecutar `mvn clean install`
  - [ ] 27.2. Verificar Swagger UI
  - [ ] 27.3. Probar flujo completo: crear categoría, crear insumo, registrar entrada, registrar salida con consumo por potrero, verificar stock actualizado
  - [ ] 27.4. Probar precio promedio ponderado
  - [ ] 27.5. Probar alertas: simular stock bajo, verificar alerta creada
  - [ ] 27.6. Probar schedulers
  - [ ] 27.7. Ejecutar todos los tests
  - [ ] 27.8. Verificar multi-tenancy

## Task Dependency Graph

```json
{
  "waves": [
    {"name": "Setup", "tasks": [1]},
    {"name": "Foundation", "tasks": [2, 20]},
    {"name": "Ports", "tasks": [3]},
    {"name": "Persistence", "tasks": [4, 5]},
    {"name": "Integration", "tasks": [6]},
    {"name": "Application Layer", "tasks": [7]},
    {"name": "Use Cases", "tasks": [8, 9, 10, 11, 12]},
    {"name": "API Contracts", "tasks": [13, 14, 16]},
    {"name": "Controllers", "tasks": [15, 17, 19]},
    {"name": "Schedulers", "tasks": [18]},
    {"name": "Testing", "tasks": [21, 22, 23, 24]},
    {"name": "Optimization", "tasks": [25]},
    {"name": "Documentation", "tasks": [26]},
    {"name": "Verification", "tasks": [27]}
  ]
}
```

## Notes

- **Transaccionalidad crítica**: Movimientos de stock DEBEN ser transaccionales
- **Precio promedio ponderado**: Solo se actualiza en entradas con precio proporcionado
- **Categorías personalizadas**: Máxima flexibilidad para usuarios
- **Consumo por potrero**: Permite análisis detallado de costos por área
- **Schedulers**: 7 AM para vencimientos, 8 AM para stock bajo
- **Multi-tenancy**: Filtrar en todas las operaciones
- **Reportes**: Considerar caché agresivo por queries costosas
- **Tests de concurrencia**: Validar que no haya condiciones de carrera en movimientos simultáneos
