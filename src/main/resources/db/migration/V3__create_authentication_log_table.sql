-- V3: Tabla authentication_log
-- Registra todos los intentos de autenticación (exitosos y fallidos) para
-- auditoría de seguridad, rate limiting y detección de bloqueo de cuentas.
-- Retención mínima de 730 días (2 años).

CREATE TABLE authentication_log (
    log_id     CHAR(36)     NOT NULL,
    email      VARCHAR(255) NOT NULL,
    timestamp  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    success    BOOLEAN      NOT NULL,
    client_ip  VARCHAR(45)  NOT NULL,
    user_agent VARCHAR(500) NOT NULL,
    PRIMARY KEY (log_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Índices para consultas comunes: rate limiting por IP, bloqueo por email, reportes por fecha
CREATE INDEX idx_auth_log_email ON authentication_log (email);
CREATE INDEX idx_auth_log_timestamp ON authentication_log (timestamp);
CREATE INDEX idx_auth_log_client_ip ON authentication_log (client_ip);
