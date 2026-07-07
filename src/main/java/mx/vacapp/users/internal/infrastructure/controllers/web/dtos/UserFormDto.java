package mx.vacapp.users.internal.infrastructure.controllers.web.dtos;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.UUID;

/**
 * DTO para formulario web de creación y edición de usuarios.
 * <p>
 * Este DTO es utilizado por controladores Thymeleaf para capturar datos de formularios
 * de creación y edición de usuarios en la interfaz web.
 * </p>
 * <p>
 * Características:
 * - El campo {@code userId} es {@code null} para formularios de creación y contiene
 *   el UUID del usuario para formularios de edición.
 * - El campo {@code password} es requerido para creación de usuarios y puede ser
 *   {@code null} para edición de usuarios (para mantener la contraseña existente).
 * - El campo {@code confirmPassword} se utiliza para confirmar la contraseña en
 *   formularios de creación, pero no se valida en formularios de edición cuando
 *   el password es {@code null}.
 * - El campo {@code status} solo es relevante para formularios de edición y
 *   permite cambiar el estado del usuario (ACTIVE, INACTIVE, LOCKED).
 * </p>
 */
public record UserFormDto(
    
    /**
     * UUID del usuario para formularios de edición, {@code null} para creación.
     * Este campo no se valida mediante Bean Validation ya que su presencia/ausencia
     * determina si se trata de una operación de creación o edición.
     */
    UUID userId,
    
    /**
     * Email del usuario. Debe ser único dentro del tenant.
     */
    @NotNull(message = "El email es obligatorio")
    @Email(message = "Debe proporcionar un email válido")
    @Size(max = 255, message = "El email no puede exceder 255 caracteres")
    String email,
    
    /**
     * Nombre completo del usuario.
     */
    @NotNull(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    String name,
    
    /**
     * Número de teléfono del usuario. Solo dígitos, entre 7 y 20 caracteres.
     */
    @NotNull(message = "El teléfono es obligatorio")
    @Size(min = 7, max = 20, message = "El teléfono debe tener entre 7 y 20 dígitos")
    @Pattern(regexp = "^[0-9]{7,20}$", message = "El teléfono debe contener solo dígitos (7-20)")
    String phone,
    
    /**
     * Contraseña del usuario.
     * <p>
     * En formularios de creación: debe ser proporcionada.
     * En formularios de edición: puede ser {@code null} para mantener la contraseña existente.
     * </p>
     * La validación de este campo se realiza a nivel de controlador según el contexto.
     */
    @Size(min = 8, max = 128, message = "La contraseña debe tener entre 8 y 128 caracteres")
    String password,
    
    /**
     * Confirmación de contraseña para formularios de creación.
     * <p>
     * Este campo no se valida mediante Bean Validation ya que la validación
     * de coincidencia con {@code password} debe realizarse a nivel de controlador.
     * </p>
     */
    String confirmPassword,
    
    /**
     * Rol del usuario en el sistema.
     * <p>
     * Roles SaaS (plataforma): super_admin, support
     * Roles Business (tenant): admin, manager, veterinarian, worker
     * </p>
     * El rol por defecto para creación es WORKER si no se especifica.
     */
    @NotNull(message = "El rol es obligatorio")
    @Pattern(
        regexp = "^(super_admin|support|admin|manager|veterinarian|worker)$",
        message = "Rol inválido. Valores permitidos: super_admin, support, admin, manager, veterinarian, worker"
    )
    String role,
    
    /**
     * Estado del usuario (solo para formularios de edición).
     * <p>
     * Este campo es opcional y permite cambiar el estado del usuario existente.
     * En formularios de creación, el estado inicial siempre es ACTIVE.
     * </p>
     */
    @Pattern(
        regexp = "^(active|inactive|locked)?$",
        message = "Estado inválido. Valores permitidos: active, inactive, locked"
    )
    String status
) {
    
    /**
     * Determina si este DTO representa una operación de creación.
     *
     * @return {@code true} si es operación de creación, {@code false} si es edición
     */
    public boolean isCreateOperation() {
        return userId == null;
    }
    
    /**
     * Determina si este DTO representa una operación de edición.
     *
     * @return {@code true} si es operación de edición, {@code false} si es creación
     */
    public boolean isEditOperation() {
        return userId != null;
    }
    
    /**
     * Verifica si el formulario requiere validación de contraseña.
     * <p>
     * La contraseña es requerida para creación, pero opcional para edición.
     * </p>
     *
     * @return {@code true} si la contraseña debe validarse, {@code false} en caso contrario
     */
    public boolean requiresPasswordValidation() {
        return isCreateOperation() || (password != null && !password.isEmpty());
    }
}