-- V4__create_geography_tables.sql
-- Tablas para el módulo Geography: Rancho → Sección → Potrero

-- =====================================================
-- Tabla: ranchos
-- =====================================================
CREATE TABLE ranchos (
    rancho_id BINARY(16) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    superficie_total DECIMAL(15, 2) NOT NULL,
    descripcion VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BINARY(16) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    
    PRIMARY KEY (rancho_id),
    
    -- Constraints
    CONSTRAINT chk_rancho_superficie_positive CHECK (superficie_total > 0),
    CONSTRAINT chk_rancho_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    
    -- Índices
    INDEX idx_ranchos_tenant (tenant_id),
    INDEX idx_ranchos_status (status),
    INDEX idx_ranchos_tenant_status (tenant_id, status),
    INDEX idx_ranchos_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Tabla: secciones
-- =====================================================
CREATE TABLE secciones (
    seccion_id BINARY(16) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    superficie DECIMAL(15, 2) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    descripcion VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BINARY(16) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    
    PRIMARY KEY (seccion_id),
    
    -- Foreign Keys
    CONSTRAINT fk_seccion_rancho FOREIGN KEY (rancho_id) 
        REFERENCES ranchos(rancho_id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT chk_seccion_superficie_positive CHECK (superficie > 0),
    CONSTRAINT chk_seccion_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    
    -- Índices
    INDEX idx_secciones_rancho (rancho_id),
    INDEX idx_secciones_tenant (tenant_id),
    INDEX idx_secciones_status (status),
    INDEX idx_secciones_rancho_tenant (rancho_id, tenant_id),
    INDEX idx_secciones_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Tabla: potreros
-- =====================================================
CREATE TABLE potreros (
    potrero_id BINARY(16) NOT NULL,
    nombre VARCHAR(100) NOT NULL,
    superficie DECIMAL(15, 2) NOT NULL,
    rancho_id BINARY(16) NOT NULL,
    seccion_id BINARY(16),
    cattle_count INT NOT NULL DEFAULT 0,
    descripcion VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    tenant_id BINARY(16) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by BINARY(16) NOT NULL,
    updated_by BINARY(16) NOT NULL,
    
    PRIMARY KEY (potrero_id),
    
    -- Foreign Keys
    CONSTRAINT fk_potrero_rancho FOREIGN KEY (rancho_id) 
        REFERENCES ranchos(rancho_id) ON DELETE RESTRICT,
    CONSTRAINT fk_potrero_seccion FOREIGN KEY (seccion_id) 
        REFERENCES secciones(seccion_id) ON DELETE RESTRICT,
    
    -- Constraints
    CONSTRAINT chk_potrero_superficie_positive CHECK (superficie > 0),
    CONSTRAINT chk_potrero_cattle_count CHECK (cattle_count >= 0),
    CONSTRAINT chk_potrero_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    
    -- Índices
    INDEX idx_potreros_rancho (rancho_id),
    INDEX idx_potreros_seccion (seccion_id),
    INDEX idx_potreros_tenant (tenant_id),
    INDEX idx_potreros_status (status),
    INDEX idx_potreros_rancho_tenant (rancho_id, tenant_id),
    INDEX idx_potreros_seccion_tenant (seccion_id, tenant_id),
    INDEX idx_potreros_nombre (nombre)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- Tabla: geography_audit
-- =====================================================
CREATE TABLE geography_audit (
    audit_id BINARY(16) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id BINARY(16) NOT NULL,
    operation_type VARCHAR(20) NOT NULL,
    timestamp TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by BINARY(16) NOT NULL,
    tenant_id BINARY(16) NOT NULL,
    old_values TEXT,
    new_values TEXT,
    reason VARCHAR(500),
    
    PRIMARY KEY (audit_id),
    
    -- Constraints
    CONSTRAINT chk_audit_entity_type CHECK (entity_type IN ('RANCHO', 'SECCION', 'POTRERO')),
    CONSTRAINT chk_audit_operation_type CHECK (operation_type IN ('CREATE', 'UPDATE', 'ARCHIVE', 'DELETE')),
    
    -- Índices
    INDEX idx_audit_entity (entity_type, entity_id),
    INDEX idx_audit_tenant (tenant_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_modified_by (modified_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
