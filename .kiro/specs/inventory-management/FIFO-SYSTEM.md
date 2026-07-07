# Sistema de Lotes con FIFO - Inventory Management

## 🎯 Cambio Implementado

El módulo de inventory-management ahora usa un **sistema de lotes con FIFO (First In, First Out)** para trazabilidad exacta de compras y consumos, similar al módulo de vacunas (vaccination-management).

---

## ¿Cómo Funciona?

### 📦 Entradas (Compras)

Cada compra crea un **lote nuevo** con su precio específico:

```http
POST /api/v1/inventory/movements
{
  "id_insumo": "maiz-uuid",
  "tipo_movimiento": "entrada",
  "motivo": "compra",
  "cantidad": 10,
  "precio_unitario": 10.00,
  "proveedor": "Forrajera Central",
  "fecha_movimiento": "2024-07-01"
}
```

**Resultado:**
- Crea lote: `MAIZ-20240701-001` con 10 kg a $10/kg
- Stock total: 10 kg
- Valor del lote: $100

---

### 📤 Salidas (Consumos) con FIFO

Cuando consumes, el sistema **automáticamente** toma de los lotes más antiguos primero:

```http
POST /api/v1/inventory/movements
{
  "id_insumo": "maiz-uuid",
  "tipo_movimiento": "salida",
  "motivo": "consumo",
  "cantidad": 15,
  "id_potrero": "potrero-norte-uuid",
  "id_usuario_responsable": "user-uuid"
}
```

**Proceso FIFO automático:**

Si tienes estos lotes:
1. `MAIZ-20240701-001`: 10 kg a $10/kg (comprado 1 julio)
2. `MAIZ-20240705-002`: 20 kg a $20/kg (comprado 5 julio)

Al consumir 15 kg, el sistema:
1. ✅ Toma **10 kg del lote 1** (a $10) → Costo: $100
2. ✅ Toma **5 kg del lote 2** (a $20) → Costo: $100
3. ✅ **Costo total**: $200

**Stock resultante:**
- Lote 1: 0 kg restantes
- Lote 2: 15 kg restantes (20 - 5)

---

## 📊 Trazabilidad Exacta

### Consultar de qué lotes salió un consumo:

```http
GET /api/v1/inventory/movements/{movimiento-id}/lot-details
```

**Respuesta:**
```json
{
  "movimiento": {
    "tipo": "salida",
    "motivo": "consumo",
    "cantidad_total": 15,
    "costo_total": 200.00,
    "fecha": "2024-07-10",
    "potrero": "Potrero Norte"
  },
  "lotes_consumidos": [
    {
      "numero_lote": "MAIZ-20240701-001",
      "fecha_compra": "2024-07-01",
      "cantidad_consumida": 10,
      "precio_unitario": 10.00,
      "costo_parcial": 100.00,
      "proveedor": "Forrajera Central"
    },
    {
      "numero_lote": "MAIZ-20240705-002",
      "fecha_compra": "2024-07-05",
      "cantidad_consumida": 5,
      "precio_unitario": 20.00,
      "costo_parcial": 100.00,
      "proveedor": "Proveedor Premium"
    }
  ]
}
```

✅ **Sabes EXACTAMENTE** de qué compra salió cada kg consumido!

---

## 🗂️ Estructura de Tablas

### 1. `supplies` - Insumo base
- Solo info general (nombre, categoría, ubicación, cantidad_mínima)
- **NO almacena stock directo** (se calcula desde lotes)

### 2. `supply_lots` - Lotes con precio
```sql
- id_lote
- id_insumo
- numero_lote (ej: "MAIZ-20240707-001")
- fecha_compra
- cantidad_disponible
- precio_unitario    ← Precio específico de ESTA compra
- fecha_vencimiento
- proveedor
```

### 3. `supply_movements` - Movimientos
```sql
- id_movimiento
- id_insumo
- id_lote (si es entrada)
- tipo_movimiento (entrada/salida)
- motivo
- cantidad
- costo_calculado (para salidas)
```

### 4. `lot_consumption_detail` - Detalle FIFO
```sql
- id_detalle
- id_movimiento
- id_lote           ← De qué lote salió
- cantidad_consumida ← Cuánto salió de este lote
- precio_unitario_lote
- costo_parcial
```

---

## ✅ Ventajas del Sistema

1. **Trazabilidad Exacta**: "Consumí 10 kg del lote a $10 y 5 kg del lote a $20"
2. **Costo Real**: El costo de consumo refleja exactamente lo que pagaste
3. **Contabilidad Precisa**: Análisis financiero correcto
4. **Análisis de Proveedores**: "¿Qué proveedor tiene mejor precio?"
5. **Control de Vencimientos**: Cada lote tiene su fecha de vencimiento
6. **FIFO Automático**: No necesitas seleccionar lotes manualmente
7. **Auditoría Completa**: Rastreo de cada kg desde compra hasta consumo

---

## 📈 Ejemplo Completo

### Día 1: Compro 10 kg a $10
```
Lote creado: MAIZ-20240701-001
- Cantidad: 10 kg
- Precio: $10/kg
- Stock total: 10 kg
```

### Día 5: Compro 20 kg a $20
```
Lote creado: MAIZ-20240705-002
- Cantidad: 20 kg
- Precio: $20/kg
- Stock total: 30 kg (10 + 20)
```

### Día 10: Consumo 15 kg
```
FIFO automático:
✓ Lote MAIZ-20240701-001: Consumo 10 kg → Costo $100
✓ Lote MAIZ-20240705-002: Consumo 5 kg → Costo $100
Total: $200

Stock restante: 15 kg (solo del lote 2)
```

### Consulta Historial
```
Compras:
- 01 Jul: 10 kg @ $10 = $100 (Forrajera Central)
- 05 Jul: 20 kg @ $20 = $400 (Proveedor Premium)

Consumos:
- 10 Jul: 15 kg = $200
  - 10 kg del lote @$10 (01 Jul)
  - 5 kg del lote @$20 (05 Jul)

Stock actual:
- Lote MAIZ-20240705-002: 15 kg @ $20 = $300
```

---

## 🔄 Diferencia vs Precio Promedio Ponderado (PPP)

### Con PPP (sistema anterior):
```
Compré 10 kg a $10 = $100
Compré 20 kg a $20 = $400
Promedio: $16.67/kg

Consumo 15 kg:
Costo: 15 × $16.67 = $250
```
❌ No sabes de qué compra específica salieron

### Con FIFO (sistema nuevo):
```
Compré 10 kg a $10 = $100
Compré 20 kg a $20 = $400

Consumo 15 kg (FIFO automático):
- 10 kg @ $10 = $100
- 5 kg @ $20 = $100
Costo: $200
```
✅ Sabes EXACTAMENTE de qué compra salió cada kg

---

## 💡 Notas Importantes

- El sistema FIFO es **automático**: No necesitas seleccionar lotes al consumir
- Cada entrada crea un lote nuevo con su precio
- Cada salida se registra con detalle de qué lotes se consumieron
- El costo de consumo es la suma real de los precios de los lotes consumidos
- El stock total se calcula como: `SUM(supply_lots.cantidad_disponible)`
- Los reportes muestran trazabilidad completa

---

## 🚀 ¿Por qué este Sistema?

Este sistema es ideal para ranchos porque:
1. ✅ Permite análisis financiero preciso
2. ✅ Facilita control de proveedores
3. ✅ Cumple requisitos de auditoría
4. ✅ Mantiene trazabilidad completa
5. ✅ Es simple de usar (FIFO automático)

**Es más robusto que PPP y más fácil de usar que gestión manual de lotes.**
