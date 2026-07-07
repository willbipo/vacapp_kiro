-- V1: Tabla users
-- Almacena los usuarios del sistema (SaaS y de negocio/tenant).

CREATE TABLE users (
    user_id       CHAR(36)      NOT NULL,
    email         VARCHAR(255)  NOT NULL,
    name          VARCHAR(100)  NOT NULL,
    phone         VARCHAR(20)   NOT NULL,
    password_hash VARCHAR(255)  NOT NULL,
    role          ENUM('super_admin', 'support', 'admin', 'manager', 'veterinarian', 'worker') NOT NULL,
    status        ENUM('active', 'inactive', 'locked') NOT NULL DEFAULT 'active',
    tenant_id     CHAR(36)      NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by    CHAR(36)      NOT NULL,
    updated_by    CHAR(36)      NOT NULL,
    PRIMARY KEY (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Unicidad de email por tenant (multi-tenancy): el mismo email puede repetirse en tenants distintos.
CREATE UNIQUE INDEX uq_users_email_tenant ON users (email, tenant_id);

-- Índices para consultas comunes
CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_created_at ON users (created_at);
