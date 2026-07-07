package mx.vacapp.users.internal.infrastructure.persistence.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA que representa la tabla authentication_log en la base de datos.
 * <p>
 * Esta clase registra todos los intentos de autenticación (exitosos y fallidos)
 * para cumplir con requisitos de seguridad, auditoría y detección de intrusiones
 * según Requirement 12.
 * </p>
 * <p>
 * Cada intento de login genera automáticamente un registro con información del usuario,
 * resultado de la autenticación, dirección IP del cliente y User-Agent del navegador.
 * Estos datos permiten:
 * <ul>
 *   <li>Rastrear accesos al sistema</li>
 *   <li>Detectar intentos de acceso no autorizados</li>
 *   <li>Implementar rate limiting y bloqueo de cuentas</li>
 *   <li>Análisis de seguridad y cumplimiento normativo</li>
 * </ul>
 * </p>
 * <p>
 * Los registros de autenticación son inmutables y se retienen por mínimo 730 días (2 años)
 * antes de permitir purga manual por super_admin.
 * </p>
 * <p>
 * Esta entidad es parte de la capa de infraestructura y NO debe ser expuesta fuera del módulo.
 * </p>
 */
@Entity
@Table(name = "authentication_log")
public class AuthLogEntity {
    
    /**
     * Identificador único del registro de autenticación.
     * <p>
     * Clave primaria de la tabla. Se genera mediante UUID para cada intento de login.
     * </p>
     */
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "log_id", nullable = false, updatable = false, length = 36)
    private UUID logId;
    
    /**
     * Email utilizado en el intento de autenticación.
     * <p>
     * Se registra exactamente como fue proporcionado en el request de login,
     * sin normalización. Permite identificar patrones de ataques y errores
     * de escritura de usuarios legítimos.
     * </p>
     * <p>
     * Nota: Este campo NO debe ser considerado sensible ya que el email es
     * el identificador público del usuario, no información confidencial.
     * </p>
     */
    @Column(name = "email", nullable = false, length = 255, updatable = false)
    private String email;
    
    /**
     * Timestamp UTC de cuándo ocurrió el intento de autenticación.
     * <p>
     * Registra el momento exacto en que se procesó el intento de login.
     * Permite análisis temporal, detección de patrones de acceso y correlación
     * de eventos de seguridad.
     * </p>
     */
    @Column(name = "timestamp", nullable = false, updatable = false)
    private Instant timestamp;
    
    /**
     * Indica si el intento de autenticación fue exitoso o fallido.
     * <p>
     * - true: Credenciales válidas, usuario autenticado correctamente, token JWT generado
     * - false: Credenciales inválidas, cuenta inactiva, cuenta bloqueada, u otro error
     * </p>
     * <p>
     * Este campo es crítico para implementar:
     * <ul>
     *   <li>Bloqueo temporal tras 5 intentos fallidos consecutivos</li>
     *   <li>Rate limiting por IP</li>
     *   <li>Alertas de seguridad ante patrones anómalos</li>
     * </ul>
     * </p>
     */
    @Column(name = "success", nullable = false, updatable = false)
    private Boolean success;
    
    /**
     * Dirección IP del cliente que realizó el intento de autenticación.
     * <p>
     * Se extrae del header X-Forwarded-For (si existe, para entornos con proxy/load balancer)
     * o de request.getRemoteAddr() directamente.
     * </p>
     * <p>
     * Permite:
     * <ul>
     *   <li>Implementar rate limiting por IP (máx 5 requests/minuto)</li>
     *   <li>Detectar ataques de fuerza bruta desde IPs específicas</li>
     *   <li>Geolocalización de accesos para análisis de seguridad</li>
     *   <li>Bloqueo de IPs sospechosas</li>
     * </ul>
     * </p>
     */
    @Column(name = "client_ip", nullable = false, length = 45, updatable = false)
    private String clientIp;
    
    /**
     * User-Agent del navegador o cliente HTTP que realizó el intento.
     * <p>
     * Se extrae del header User-Agent del request HTTP.
     * Truncado a 500 caracteres para evitar valores excesivos.
     * </p>
     * <p>
     * Permite:
     * <ul>
     *   <li>Identificar el tipo de dispositivo y navegador utilizado</li>
     *   <li>Detectar bots y herramientas automatizadas de ataque</li>
     *   <li>Análisis de uso de la plataforma por tipo de cliente</li>
     *   <li>Correlación de sesiones sospechosas</li>
     * </ul>
     * </p>
     */
    @Column(name = "user_agent", nullable = false, length = 500, updatable = false)
    private String userAgent;
    
    // Constructor sin argumentos requerido por JPA
    public AuthLogEntity() {
    }
    
    // Constructor con todos los argumentos para facilitar creación
    public AuthLogEntity(UUID logId, String email, Instant timestamp, Boolean success, 
                        String clientIp, String userAgent) {
        this.logId = logId;
        this.email = email;
        this.timestamp = timestamp;
        this.success = success;
        this.clientIp = clientIp;
        this.userAgent = userAgent;
    }
    
    // Getters y Setters
    public UUID getLogId() {
        return logId;
    }
    
    public void setLogId(UUID logId) {
        this.logId = logId;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
    
    public Boolean getSuccess() {
        return success;
    }
    
    public void setSuccess(Boolean success) {
        this.success = success;
    }
    
    public String getClientIp() {
        return clientIp;
    }
    
    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }
    
    public String getUserAgent() {
        return userAgent;
    }
    
    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
