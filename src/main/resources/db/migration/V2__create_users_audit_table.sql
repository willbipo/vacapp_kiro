-- V2: Tabla users_audit
-- Registra el historial de operaciones (CREATE, UPDATE, DEACTIVATE) sobre usuarios.
-- Retención mínima de 730 días (2 años) según requisitos de auditoría.

CREATE TABLE users_audit (
    audit_id       CHAR(36)     NOT NULL,
    user_id        CHAR(36)     NOT NULL,
    timestamp      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    modified_by    CHAR(36)     NOT NULL,
    operation_type ENUM('CREATE', 'UPDATE', 'DEACTIVATE') NOT NULL,
    old_values     TEXT         NULL,
    new_values     TEXT         NOT NULL,
    PRIMARY KEY (audit_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para consultas comunes de auditoría
CREATE INDEX idx_users_audit_user_id ON users_audit (user_id);
CREATE INDEX idx_users_audit_timestamp ON users_audit (timestamp);
