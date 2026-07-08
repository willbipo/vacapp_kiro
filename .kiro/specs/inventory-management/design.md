# Design Document: Inventory Management Module

## Overview

El módulo **inventory-management** proporciona gestión completa del inventario de insumos del rancho en Vacapp. Implementa categorías personalizables por usuario, control de stock con movimientos rastreables (entradas/salidas), alertas automáticas de stock bajo y vencimientos, consumo vinculado a potreros y actividades, reportes de valoración y costos, y auditoría completa de operaciones. Se integra con geographic-control para asignar consumo a potreros y con user-management para rastrear responsables.

### Contexto

Vacapp es un monolito modular construido con Spring Modulith siguiendo Clean Architecture. El módulo inventory-management debe implementarse después de user-management y geographic-control ya que depende de autenticación JWT y validación de potreros activos. Este módulo es fundamental para control de costos operativos y gestión de recursos del rancho.

### Objetivos del Módulo

1. **Categorías personalizadas**: Usuarios crean sus propias categorías de insumos
2. **Control de stock**: Movimientos de entrada/salida con actualización automática
3. **Trazabilidad completa**: Registro de responsable, motivo, fecha de cada movimiento
4. **Consumo por potrero**: Vincular uso de insumos a áreas específicas
5. **Alertas inteligentes**: Stock bajo y vencimientos próximos
6. **Valoración de inventario**: Cálculo de valor monetario total
7. **Reportes de consumo**: Análisis de uso y costos por periodo
8. **Multi-tenancy**: Aislamiento por tenant_id y rancho_id

### Tecnologías

- **Backend**: Java 21, Spring Boot 4.1.0, Spring Modulith
- **Seguridad**: Spring Security 6 (integración con user-management)
- **Persistencia**: Spring Data JPA, MySQL 8
- **Validación**: Bean Validation (Hibernate Validator)
- **Scheduling**: Spring Scheduled Tasks para alertas
- **Caching**: Spring Cache con Caffeine
- **API Documentation**: OpenAPI 3.0 (Design-First con YAML)
- **Frontend Web**: Thymeleaf, HTML5, CSS Grid, JavaScript Vanilla
- **Utilidades**: Lombok

## Architecture

### Arquitectura de Capas (Clean Architecture)

```
mx.vacapp.inventory/
│
├── InventoryService.java          ← API PÚBLICA
│
└── internal/                       ← TODO lo demás es PRIVADO
    │
    ├── domain/
    │   ├── model/
    │   │   ├── Supply.java            ← Entidad principal de insumo
    │   │   ├── SupplyCategory.java    ← Categoría personalizada
    │   │   ├── MeasurementUnit.java   ← Unidad de medida
    │   │   ├── SupplyMovement.java    ← Movimiento de inventario
    │   │   ├── MovementType.java      ← Enum (ENTRADA, SALIDA)
    │   │   ├── MovementReason.java    ← Enum (COMPRA, CONSUMO, MERMA, etc.)
    │   │   ├── StockStatus.java       ← Enum (OK, BAJO, CRITICO)
    │   │   ├── StockAlert.java        ← Alerta de stock bajo
    │   │   ├── ExpirationAlert.java   ← Alerta de vencimiento
    │   │   ├── PastureConsumption.java ← Consumo vinculado a potrero
    │   │   └── exceptions/
    │   │       ├── SupplyNotFoundException.java
    │   │       ├── InsufficientStockException.java
    │   │       ├── CategoryNotFoundException.java
    │   │       ├── InvalidMovementException.java
    │   │       └── ExpiredSupplyException.java
    │   │
    │   └── repository/
    │       ├── SupplyRepository.java
    │       ├── SupplyCategoryRepository.java
    │       ├── SupplyMovementRepository.java
    │       ├── StockAlertRepository.java
    │       ├── ExpirationAlertRepository.java
    │       ├── PastureConsumptionRepository.java
    │       └── InventoryAuditRepository.java
    │
    ├── application/
    │   └── usecases/
    │       ├── category/
    │       │   ├── CreateCategoryUseCase.java
    │       │   ├── UpdateCategoryUseCase.java
    │       │   ├── GetCategoryUseCase.java
    │       │   ├── ListCategoriesUseCase.java
    │       │   └── ArchiveCategoryUseCase.java
    │       ├── supply/
    │       │   ├── CreateSupplyUseCase.java
    │       │   ├── UpdateSupplyUseCase.java
    │       │   ├── GetSupplyUseCase.java
    │       │   ├── ListSuppliesUseCase.java
    │       │   ├── SearchSuppliesUseCase.java
    │       │   └── ArchiveSupplyUseCase.java
    │       ├── movement/
    │       │   ├── RegisterStockInUseCase.java
    │       │   ├── RegisterStockOutUseCase.java
    │       │   ├── GetMovementHistoryUseCase.java
    │       │   └── GetMovementsByPeriodUseCase.java
    │       ├── alert/
    │       │   ├── CheckStockAlertsUseCase.java
    │       │   ├── CheckExpirationAlertsUseCase.java
    │       │   ├── GetActiveStockAlertsUseCase.java
    │       │   └── GetActiveExpirationAlertsUseCase.java
    │       ├── report/
    │       │   ├── GenerateInventoryValuationUseCase.java
    │       │   ├── GenerateConsumptionReportUseCase.java
    │       │   ├── GenerateCostReportUseCase.java
    │       │   └── GetConsumptionByPastureUseCase.java
    │       └── commands/
    │           ├── CreateCategoryCommand.java
    │           ├── CategoryResult.java
    │           ├── CreateSupplyCommand.java
    │           ├── SupplyResult.java
    │           ├── RegisterMovementCommand.java
    │           ├── MovementResult.java
    │           ├── ValuationReportResult.java
    │           └── ConsumptionReportResult.java
    │
    └── infrastructure/
        │
        ├── controllers/
        │   ├── mobile/
        │   │   ├── SupplyCategoryRestController.java
        │   │   ├── SupplyRestController.java
        │   │   ├── MovementRestController.java
        │   │   ├── AlertRestController.java
        │   │   ├── ReportRestController.java
        │   │   └── dtos/
        │   │       ├── CreateCategoryRequest.java
        │   │       ├── UpdateCategoryRequest.java
        │   │       ├── CategoryResponse.java
        │   │       ├── CategoryDetailResponse.java
        │   │       ├── CreateSupplyRequest.java
        │   │       ├── SupplyResponse.java
        │   │       ├── SupplyDetailResponse.java
        │   │       ├── RegisterMovementRequest.java
        │   │       ├── MovementResponse.java
        │   │       ├── StockAlertResponse.java
        │   │       ├── ExpirationAlertResponse.java
        │   │       ├── ValuationReportResponse.java
        │   │       └── ConsumptionReportResponse.java
        │   │
        │   └── web/
        │       ├── InventoryWebController.java
        │       ├── MovementWebController.java
        │       └── dtos/
        │           ├── SupplyFormDto.java
        │           └── MovementFormDto.java
        │
        ├── persistence/
        │   ├── entities/
        │   │   ├── SupplyEntity.java
        │   │   ├── SupplyCategoryEntity.java
        │   │   ├── MeasurementUnitEntity.java
        │   │   ├── SupplyMovementEntity.java
        │   │   ├── StockAlertEntity.java
        │   │   ├── ExpirationAlertEntity.java
        │   │   ├── PastureConsumptionEntity.java
        │   │   └── InventoryAuditEntity.java
        │   ├── repositories/
        │   │   ├── SupplyJpaRepository.java
        │   │   ├── SupplyCategoryJpaRepository.java
        │   │   ├── SupplyMovementJpaRepository.java
        │   │   ├── StockAlertJpaRepository.java
        │   │   └── ExpirationAlertJpaRepository.java
        │   ├── impl/
        │   │   ├── SupplyRepositoryImpl.java
        │   │   ├── SupplyCategoryRepositoryImpl.java
        │   │   ├── SupplyMovementRepositoryImpl.java
        │   │   ├── StockAlertRepositoryImpl.java
        │   │   └── ExpirationAlertRepositoryImpl.java
        │   └── mappers/
        │       ├── SupplyMapper.java
        │       ├── SupplyCategoryMapper.java
        │       └── SupplyMovementMapper.java
        │
        ├── integration/
        │   ├── GeographyServiceClient.java
        │   └── UserServiceClient.java
        │
        ├── scheduling/
        │   ├── StockAlertScheduler.java
        │   └── ExpirationAlertScheduler.java
        │
        └── config/
            ├── InventoryModuleConfig.java
            └── SchedulingConfig.java
```

### Flujo de Datos

#### Flujo de Entrada de Insumo (Stock In con Creación de Lote)

```
1. Usuario envía POST /api/v1/inventory/movements con {id_insumo, tipo_movimiento: ENTRADA, motivo: COMPRA, cantidad: 20, precio_unitario: 20, proveedor: "Forrajera XYZ"}
   ↓
2. MovementRestController recibe RegisterMovementRequest, valida @Valid
   ↓
3. MovementRestController mapea a RegisterMovementCommand
   ↓
4. RegisterStockInUseCase.execute(command)
   ↓
5. SupplyRepository.findById(id_insumo) → obtener insumo, validar tenant
   ↓
6. BEGIN TRANSACTION
   |
   |-- Generar numero_lote automático: "{NOMBRE_INSUMO}-{YYYYMMDD}-{SEQUENCE}"
   |   Ejemplo: "MAIZ-20240707-002"
   |
   |-- INSERT INTO supply_lots (
   |     id_lote, id_insumo, numero_lote, fecha_compra, cantidad_disponible,
   |     precio_unitario, proveedor, fecha_vencimiento (si se proporciona), ...
   |   ) VALUES (
   |     UUID(), id_insumo, numero_lote_generado, fecha_movimiento, cantidad,
   |     precio_unitario, proveedor, fecha_vencimiento, ...
   |   )
   |
   |-- INSERT INTO supply_movements (
   |     id_movimiento, id_insumo, id_lote, tipo_movimiento: ENTRADA,
   |     motivo: COMPRA, cantidad, precio_unitario, fecha_movimiento, ...
   |   )
   |
   |-- nuevo_stock_total = SUM(supply_lots.cantidad_disponible WHERE id_insumo = :id)
   |
   |-- IF nuevo_stock_total > supply.cantidad_minima AND 
   |      EXISTS(stock_alerts WHERE id_insumo = :id AND resuelta = FALSE):
   |   |  UPDATE stock_alerts SET resuelta = TRUE, fecha_resolucion = NOW()
   |
   |-- InventoryAuditRepository.logStockIn(...)
   |
   COMMIT TRANSACTION
   ↓
7. Retornar MovementResult con:
    - id_movimiento
    - id_lote
    - numero_lote
    - nuevo_stock_total
    - valor_lote (cantidad * precio_unitario)
   ↓
8. ResponseEntity.status(201) → HTTP 201
```

#### Flujo de Salida de Insumo (Stock Out con FIFO y Consumo por Potrero)

```
1. Usuario envía POST /api/v1/inventory/movements con {id_insumo, tipo_movimiento: SALIDA, motivo: CONSUMO, cantidad: 15, id_potrero, id_usuario_responsable}
   ↓
2. RegisterStockOutUseCase.execute(command)
   ↓
3. SupplyRepository.findById(id_insumo) → obtener insumo
   ↓
4. SupplyLotRepository.findBySupplyOrderByFechaCompraAsc(id_insumo) → obtener lotes disponibles ordenados por FIFO
   ↓
5. cantidad_total = SUM(lotes.cantidad_disponible)
   ↓
6. IF cantidad_total < cantidad_solicitada:
   |  THROW InsufficientStockException("Stock insuficiente")
   ↓
7. IF id_potrero PROVIDED:
   |  GeographyService.isPotreroActive(id_potrero) → validar potrero
   ↓
8. UserService.exists(id_usuario_responsable) → validar usuario
   ↓
9. BEGIN TRANSACTION
   |
   |-- CREATE supply_movement registro base
   |
   |-- cantidad_restante = cantidad_solicitada
   |-- costo_total = 0
   |-- lotes_consumidos = []
   |
   |-- FOR EACH lote IN lotes (ordenados por fecha_compra ASC):
   |   |
   |   |  IF cantidad_restante <= 0: BREAK
   |   |
   |   |  cantidad_a_consumir = MIN(lote.cantidad_disponible, cantidad_restante)
   |   |  
   |   |  UPDATE supply_lots 
   |   |  SET cantidad_disponible = cantidad_disponible - cantidad_a_consumir
   |   |  WHERE id_lote = lote.id
   |   |
   |   |  costo_parcial = cantidad_a_consumir * lote.precio_unitario
   |   |  costo_total += costo_parcial
   |   |
   |   |  INSERT INTO lot_consumption_detail (
   |   |    id_detalle, id_movimiento, id_lote, cantidad_consumida,
   |   |    precio_unitario_lote, costo_parcial
   |   |  )
   |   |
   |   |  lotes_consumidos.add({
   |   |    lote_id, numero_lote, cantidad_consumida: cantidad_a_consumir,
   |   |    precio: lote.precio_unitario, costo: costo_parcial
   |   |  })
   |   |
   |   |  cantidad_restante -= cantidad_a_consumir
   |
   |-- UPDATE supply_movements SET costo_calculado = costo_total
   |
   |-- IF id_potrero PROVIDED:
   |   |  INSERT INTO pasture_supply_consumption (...)
   |
   |-- nuevo_stock_total = SUM(supply_lots.cantidad_disponible WHERE id_insumo = :id)
   |
   |-- IF nuevo_stock_total <= supply.cantidad_minima:
   |   |  nivel_alerta = (nuevo_stock_total <= supply.cantidad_minima * 0.5) ? CRITICO : BAJO
   |   |  INSERT INTO stock_alerts (...)
   |
   |-- InventoryAuditRepository.logStockOut(lotes_consumidos)
   |
   COMMIT TRANSACTION
   ↓
10. Retornar MovementResult con:
    - nuevo_stock_total
    - costo_total_calculado
    - lotes_consumidos: [
        {numero_lote: "MAIZ-20240701-001", cantidad: 10, precio: $10, costo: $100},
        {numero_lote: "MAIZ-20240705-002", cantidad: 5, precio: $20, costo: $100}
      ]
   ↓
11. ResponseEntity.status(201) → HTTP 201
```

#### Flujo de Alerta de Vencimiento (Scheduler)

```
@Scheduled(cron = "0 0 7 * * *")  // Cada día a las 7:00 AM
CheckExpirationAlertsUseCase.execute():
  |
  insumos_por_vencer = SupplyRepository.findExpiringWithinDays(30)
  |
  FOR EACH insumo IN insumos_por_vencer:
  |  |
  |  dias_para_vencer = DATEDIFF(insumo.fecha_vencimiento, CURRENT_DATE)
  |  |
  |  nivel_urgencia = CASE
  |                     WHEN dias_para_vencer <= 7 THEN URGENTE
  |                     WHEN dias_para_vencer <= 15 THEN PROXIMO
  |                     ELSE PREVENTIVO
  |                   END
  |  |
  |  alerta_existente = ExpirationAlertRepository.findBySupply(insumo.id)
  |  |
  |  IF alerta_existente IS NULL:
  |  |  INSERT INTO expiration_alerts (id_alerta, id_insumo, fecha_vencimiento,
  |  |                                  dias_para_vencer, cantidad_en_riesgo, nivel_urgencia, ...)
  |  ELSE:
  |  |  UPDATE expiration_alerts SET dias_para_vencer = :dias, nivel_urgencia = :nivel
  |
  // Marcar insumos vencidos
  UPDATE supplies SET vencido = TRUE WHERE fecha_vencimiento < CURRENT_DATE
```

### Principios de Diseño

1. **Transaccionalidad**: Movimientos de stock en transacciones atómicas
2. **Trazabilidad**: Registro de responsable, motivo, fecha en cada movimiento
3. **Precio promedio ponderado**: Actualización automática al recibir entradas
4. **Alertas proactivas**: Jobs programados para detectar problemas
5. **Flexibilidad**: Categorías personalizables por usuario
6. **Multi-tenancy**: Filtrado automático por tenant_id y rancho_id

## Components and Interfaces

### 1. API Pública del Módulo

#### InventoryService.java

```java
package mx.vacapp.inventory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * API pública del módulo de gestión de inventario.
 */
public interface InventoryService {
    
    /**
     * Obtiene consumo de insumos por potrero en un periodo.
     */
    List<SupplyConsumption> getSupplyConsumptionByPasture(
        UUID potreroId, 
        LocalDate fechaInicio, 
        LocalDate fechaFin
    );
    
    /**
     * Calcula costo total de consumo de insumos por potrero.
     */
    BigDecimal getTotalCostByPasture(
        UUID potreroId,
        LocalDate fechaInicio,
        LocalDate fechaFin
    );
    
    /**
     * Verifica si un insumo existe y está activo.
     */
    boolean isSupplyActive(UUID supplyId);
    
    record SupplyConsumption(
        String supplyName,
        BigDecimal totalQuantity,
        String unit,
        BigDecimal totalCost
    ) {}
}
```

### 2. Capa de Dominio

#### Supply.java (Entidad de Dominio)

```java
package mx.vacapp.inventory.internal.domain.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Entidad de dominio que representa un insumo del inventario.
 * El stock total se calcula desde los lotes asociados.
 */
public class Supply {
    private final UUID supplyId;
    private final String nombre;
    private final UUID categoryId;
    private final UUID unitId;
    private final BigDecimal cantidadMinima;
    private final String descripcion;
    private final String ubicacion;
    private final UUID ranchoId;
    private final UUID tenantId;
    private final boolean activo;
    
    // El stock total se calcula desde los lotes
    public BigDecimal getCantidadTotal(List<SupplyLot> lots) {
        return lots.stream()
            .filter(SupplyLot::hasStock)
            .map(SupplyLot::getCantidadDisponible)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    
    public StockStatus getStockStatus(BigDecimal cantidadTotal) {
        if (cantidadTotal.compareTo(cantidadMinima.multiply(BigDecimal.valueOf(0.5))) <= 0) {
            return StockStatus.CRITICO;
        }
        if (cantidadTotal.compareTo(cantidadMinima) <= 0) {
            return StockStatus.BAJO;
        }
        return StockStatus.OK;
    }
    
    public BigDecimal getValorTotal(List<SupplyLot> lots) {
        return lots.stream()
            .filter(SupplyLot::hasStock)
            .map(SupplyLot::getValorLote)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
```

#### SupplyLot.java

```java
package mx.vacapp.inventory.internal.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Lote específico de un insumo con precio y fecha de compra.
 */
public class SupplyLot {
    private final UUID lotId;
    private final UUID supplyId;
    private final String numeroLote;
    private final LocalDate fechaCompra;
    private BigDecimal cantidadDisponible;
    private final BigDecimal precioUnitario;
    private final LocalDate fechaVencimiento;
    private final String proveedor;
    
    public boolean isExpired(LocalDate currentDate) {
        return fechaVencimiento != null && fechaVencimiento.isBefore(currentDate);
    }
    
    public boolean hasStock() {
        return cantidadDisponible.compareTo(BigDecimal.ZERO) > 0;
    }
    
    public void decrementStock(BigDecimal quantity) {
        if (cantidadDisponible.compareTo(quantity) < 0) {
            throw new InsufficientStockException("Stock insuficiente en lote");
        }
        this.cantidadDisponible = this.cantidadDisponible.subtract(quantity);
    }
    
    public BigDecimal getValorLote() {
        return cantidadDisponible.multiply(precioUnitario);
    }
    
    public int daysUntilExpiration(LocalDate currentDate) {
        if (fechaVencimiento == null) return -1;
        return (int) currentDate.until(fechaVencimiento, java.time.temporal.ChronoUnit.DAYS);
    }
}
```

#### LotConsumptionDetail.java

```java
package mx.vacapp.inventory.internal.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Detalle de qué lotes fueron consumidos en un movimiento de salida.
 */
public class LotConsumptionDetail {
    private final UUID detailId;
    private final UUID movementId;
    private final UUID lotId;
    private final BigDecimal cantidadConsumida;
    private final BigDecimal precioUnitarioLote;
    private final BigDecimal costoParcial;
    
    public static LotConsumptionDetail create(
        UUID movementId, 
        UUID lotId, 
        BigDecimal cantidad, 
        BigDecimal precioUnitario
    ) {
        BigDecimal costo = cantidad.multiply(precioUnitario);
        return new LotConsumptionDetail(
            UUID.randomUUID(),
            movementId,
            lotId,
            cantidad,
            precioUnitario,
            costo
        );
    }
}
```

#### Enums de Dominio

```java
public enum MovementType {
    ENTRADA("entrada"),
    SALIDA("salida");
    
    private final String value;
    MovementType(String value) { this.value = value; }
}

public enum MovementReason {
    // Razones de entrada
    COMPRA("compra"),
    DONACION("donacion"),
    DEVOLUCION("devolucion"),
    AJUSTE_ENTRADA("ajuste_entrada"),
    
    // Razones de salida
    CONSUMO("consumo"),
    MERMA("merma"),
    AJUSTE_SALIDA("ajuste_salida");
    
    private final String value;
    MovementReason(String value) { this.value = value; }
    
    public boolean isEntryReason() {
        return this == COMPRA || this == DONACION || this == DEVOLUCION || this == AJUSTE_ENTRADA;
    }
    
    public boolean isExitReason() {
        return this == CONSUMO || this == MERMA || this == AJUSTE_SALIDA;
    }
}

public enum StockStatus {
    OK("ok"),
    BAJO("bajo"),
    CRITICO("critico");
    
    private final String value;
    StockStatus(String value) { this.value = value; }
}
```

## Data Models

### Esquema de Base de Datos MySQL

```sql
-- Tabla de unidades de medida (predefinidas)
CREATE TABLE measurement_units (
    id_unidad BINARY(16) PRIMARY KEY,
    nombre VARCHAR(50) NOT NULL UNIQUE,
    abreviatura VARCHAR(10) NOT NULL,
    tipo ENUM('Peso', 'Volumen', 'Cantidad', 'Longitud') NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    
    INDEX idx_measurement_units_activo (activo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Insertar unidades predefinidas
INSERT INTO measurement_units (id_unidad, nombre, abreviatura, tipo) VALUES
(UUID_TO_BIN(UUID()), 'Kilogramo', 'kg', 'Peso'),
(UUID_TO_BIN(UUID()), 'Gramo', 'g', 'Peso'),
(UUID_TO_BIN(UUID()), 'Tonelada', 'ton', 'Peso'),
(UUID_TO_BIN(UUID()), 'Litro', 'L', 'Volumen'),
(UUID_TO_BIN(UUID()), 'Mililitro', 'mL', 'Volumen'),
(UUID_TO_BIN(UUID()), 'Galón', 'gal', 'Volumen'),
(UUID_TO_BIN(UUID()), 'Unidad', 'unid', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Pieza', 'pza', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Saco', 'saco', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Caja', 'caja', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Paquete', 'paq', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Bulto', 'bulto', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Dosis', 'dosis', 'Cantidad'),
(UUID_TO_BIN(UUID()), 'Metro', 'm', 'Longitud');

-- Tabla de categorías de insumos (personalizables por usuario)
CREATE TABLE supply_categories (
    id_categoria BINARY(16) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT,
    color VARCHAR(7),  -- Hex color code
    icono VARCHAR(50),
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    INDEX idx_supply_categories_tenant (tenant_id, rancho_id),
    INDEX idx_supply_categories_activo (activo),
    UNIQUE KEY uk_category_name_rancho (nombre, rancho_id, tenant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla principal de insumos (sin stock directo, se calcula desde lotes)
CREATE TABLE supplies (
    id_insumo BINARY(16) PRIMARY KEY,
    nombre VARCHAR(200) NOT NULL,
    id_categoria BINARY(16) NOT NULL,
    id_unidad_medida BINARY(16) NOT NULL,
    cantidad_minima DECIMAL(10,3) NOT NULL DEFAULT 0,
    descripcion TEXT,
    ubicacion VARCHAR(200),
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    
    CONSTRAINT chk_cantidad_minima CHECK (cantidad_minima >= 0),
    
    INDEX idx_supplies_tenant (tenant_id, rancho_id),
    INDEX idx_supplies_categoria (id_categoria),
    INDEX idx_supplies_activo (activo),
    INDEX idx_supplies_nombre (nombre),
    
    CONSTRAINT fk_supplies_categoria FOREIGN KEY (id_categoria) REFERENCES supply_categories(id_categoria),
    CONSTRAINT fk_supplies_unidad FOREIGN KEY (id_unidad_medida) REFERENCES measurement_units(id_unidad)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de lotes de insumos
CREATE TABLE supply_lots (
    id_lote BINARY(16) PRIMARY KEY,
    id_insumo BINARY(16) NOT NULL,
    numero_lote VARCHAR(50) NOT NULL,
    fecha_compra DATE NOT NULL,
    cantidad_disponible DECIMAL(10,3) NOT NULL DEFAULT 0,
    precio_unitario DECIMAL(10,2) NOT NULL,
    fecha_vencimiento DATE,
    proveedor VARCHAR(200),
    observaciones TEXT,
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    CONSTRAINT chk_cantidad_disponible CHECK (cantidad_disponible >= 0),
    CONSTRAINT chk_precio_unitario CHECK (precio_unitario > 0),
    
    INDEX idx_supply_lots_insumo (id_insumo),
    INDEX idx_supply_lots_fecha_compra (id_insumo, fecha_compra),
    INDEX idx_supply_lots_vencimiento (fecha_vencimiento),
    INDEX idx_supply_lots_tenant (tenant_id, rancho_id),
    UNIQUE KEY uk_numero_lote (numero_lote, rancho_id, tenant_id),
    
    CONSTRAINT fk_supply_lots_insumo FOREIGN KEY (id_insumo) REFERENCES supplies(id_insumo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de movimientos de inventario
CREATE TABLE supply_movements (
    id_movimiento BINARY(16) PRIMARY KEY,
    id_insumo BINARY(16) NOT NULL,
    id_lote BINARY(16),  -- FK al lote creado (si es entrada) o NULL (si es salida múltiple)
    tipo_movimiento ENUM('entrada', 'salida') NOT NULL,
    motivo ENUM('compra', 'donacion', 'devolucion', 'ajuste_entrada', 
                'consumo', 'merma', 'ajuste_salida') NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL,
    precio_unitario DECIMAL(10,2),  -- Solo para entradas
    costo_calculado DECIMAL(12,2),  -- Solo para salidas (calculado desde lotes consumidos)
    fecha_movimiento DATE NOT NULL,
    id_potrero BINARY(16),
    id_usuario_responsable BINARY(16),
    observaciones TEXT,
    rancho_id BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    CONSTRAINT chk_cantidad_movimiento CHECK (cantidad > 0),
    
    INDEX idx_supply_movements_insumo (id_insumo),
    INDEX idx_supply_movements_lote (id_lote),
    INDEX idx_supply_movements_tipo (tipo_movimiento),
    INDEX idx_supply_movements_fecha (fecha_movimiento),
    INDEX idx_supply_movements_tenant (tenant_id, rancho_id),
    INDEX idx_supply_movements_potrero (id_potrero),
    
    CONSTRAINT fk_supply_movements_insumo FOREIGN KEY (id_insumo) REFERENCES supplies(id_insumo),
    CONSTRAINT fk_supply_movements_lote FOREIGN KEY (id_lote) REFERENCES supply_lots(id_lote)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de detalle de consumo de lotes (para trazabilidad FIFO)
CREATE TABLE lot_consumption_detail (
    id_detalle BINARY(16) PRIMARY KEY,
    id_movimiento BINARY(16) NOT NULL,
    id_lote BINARY(16) NOT NULL,
    cantidad_consumida DECIMAL(10,3) NOT NULL,
    precio_unitario_lote DECIMAL(10,2) NOT NULL,
    costo_parcial DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    INDEX idx_lot_consumption_movement (id_movimiento),
    INDEX idx_lot_consumption_lote (id_lote),
    
    CONSTRAINT fk_lot_consumption_movement FOREIGN KEY (id_movimiento) REFERENCES supply_movements(id_movimiento),
    CONSTRAINT fk_lot_consumption_lote FOREIGN KEY (id_lote) REFERENCES supply_lots(id_lote)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de ajustes de lotes
CREATE TABLE lot_adjustments (
    id_ajuste BINARY(16) PRIMARY KEY,
    id_lote BINARY(16) NOT NULL,
    cantidad_anterior DECIMAL(10,3) NOT NULL,
    cantidad_nueva DECIMAL(10,3) NOT NULL,
    motivo VARCHAR(200) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_by BINARY(16) NOT NULL,
    
    INDEX idx_lot_adjustments_lote (id_lote),
    INDEX idx_lot_adjustments_fecha (created_at),
    
    CONSTRAINT fk_lot_adjustments_lote FOREIGN KEY (id_lote) REFERENCES supply_lots(id_lote)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de consumo vinculado a potreros
CREATE TABLE pasture_supply_consumption (
    id_consumo BINARY(16) PRIMARY KEY,
    id_movimiento BINARY(16) NOT NULL,
    id_potrero BINARY(16) NOT NULL,
    cantidad DECIMAL(10,3) NOT NULL,
    fecha_consumo DATE NOT NULL,
    id_usuario_responsable BINARY(16),
    tenant_id BINARY(16) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    
    INDEX idx_pasture_consumption_potrero (id_potrero),
    INDEX idx_pasture_consumption_fecha (fecha_consumo),
    INDEX idx_pasture_consumption_tenant (tenant_id, rancho_id),
    
    CONSTRAINT fk_pasture_consumption_movement FOREIGN KEY (id_movimiento) REFERENCES supply_movements(id_movimiento)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de alertas de stock bajo
CREATE TABLE stock_alerts (
    id_alerta BINARY(16) PRIMARY KEY,
    id_insumo BINARY(16) NOT NULL,
    cantidad_actual DECIMAL(10,3) NOT NULL,
    cantidad_minima DECIMAL(10,3) NOT NULL,
    nivel_alerta ENUM('bajo', 'critico') NOT NULL,
    fecha_alerta TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    resuelta BOOLEAN NOT NULL DEFAULT FALSE,
    fecha_resolucion TIMESTAMP(6),
    tenant_id BINARY(16) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    
    INDEX idx_stock_alerts_insumo (id_insumo),
    INDEX idx_stock_alerts_resuelta (resuelta),
    INDEX idx_stock_alerts_tenant (tenant_id, rancho_id),
    
    CONSTRAINT fk_stock_alerts_insumo FOREIGN KEY (id_insumo) REFERENCES supplies(id_insumo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de alertas de vencimiento
CREATE TABLE expiration_alerts (
    id_alerta BINARY(16) PRIMARY KEY,
    id_insumo BINARY(16) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    dias_para_vencer INT NOT NULL,
    cantidad_en_riesgo DECIMAL(10,3) NOT NULL,
    nivel_urgencia ENUM('urgente', 'proximo', 'preventivo') NOT NULL,
    fecha_alerta TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    tenant_id BINARY(16) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    
    INDEX idx_expiration_alerts_insumo (id_insumo),
    INDEX idx_expiration_alerts_dias (dias_para_vencer),
    INDEX idx_expiration_alerts_tenant (tenant_id, rancho_id),
    
    CONSTRAINT fk_expiration_alerts_insumo FOREIGN KEY (id_insumo) REFERENCES supplies(id_insumo)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Tabla de auditoría
CREATE TABLE inventory_audit (
    audit_id BINARY(16) PRIMARY KEY,
    operation_type VARCHAR(30) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    timestamp TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    modified_by BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    
    CONSTRAINT chk_operation_type CHECK (operation_type IN ('CREATE_SUPPLY', 'UPDATE_SUPPLY', 
                                                             'STOCK_IN', 'STOCK_OUT', 'CREATE_CATEGORY')),
    
    INDEX idx_inventory_audit_entity (entity_id),
    INDEX idx_inventory_audit_timestamp (timestamp)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

## Error Handling

```java
public class InventoryDomainException extends RuntimeException {}
public class SupplyNotFoundException extends InventoryDomainException {}
public class InsufficientStockException extends InventoryDomainException {}
public class CategoryNotFoundException extends InventoryDomainException {}
public class InvalidMovementException extends InventoryDomainException {}
public class ExpiredSupplyException extends InventoryDomainException {}
```

## Testing Strategy

1. **Pruebas Unitarias**: Lógica de precio promedio ponderado, clasificación de stock status
2. **Pruebas de Integración**: Movimientos transaccionales, alertas automáticas
3. **Pruebas de Concurrencia**: Movimientos simultáneos de stock
4. **Pruebas de Schedulers**: Jobs de alertas

## Notes

- Los movimientos de stock DEBEN ser transaccionales
- El precio promedio ponderado se calcula solo en entradas con precio
- Las categorías son personalizables para máxima flexibilidad
- Los schedulers ejecutan a las 7 AM para alertas de vencimiento y 8 AM para stock bajo
- El consumo vinculado a potreros permite análisis detallado de costos
- La valoración de inventario es crítica para reportes financieros
