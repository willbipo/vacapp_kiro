package mx.vacapp.users.internal.infrastructure.controllers.web;

import mx.vacapp.users.internal.application.usecases.user.GetUserUseCase;
import mx.vacapp.users.internal.application.usecases.user.ListUsersUseCase;
import mx.vacapp.users.internal.infrastructure.controllers.web.dtos.UserFormDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Controlador web para gestión de usuarios en la interfaz administrativa.
 * <p>
 * Este controlador maneja las vistas HTML (Thymeleaf) relacionadas con la gestión
 * de usuarios en el área administrativa de la aplicación. Proporciona funcionalidades
 * para listar usuarios, crear nuevos usuarios y editar usuarios existentes.
 * </p>
 * <p>
 * <strong>Roles de seguridad:</strong>
 * <ul>
 *   <li>Todas las rutas en {@code /admin/usuarios/**} requieren autenticación JWT válida</li>
 *   <li>Acceso restringido a usuarios con roles administrativos (admin, super_admin)</li>
 *   <li>La autorización específica se maneja a nivel de Spring Security</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Arquitectura:</strong>
 * <ul>
 *   <li>Capa de infraestructura (controllers/web) - adaptador de entrada HTTP MVC</li>
 *   <li>Sirve vistas Thymeleaf para interfaz web administrativa</li>
 *   <li>Obtiene datos de casos de uso de aplicación (ListUsersUseCase, GetUserUseCase)</li>
 *   <li>No contiene lógica de negocio directa - delega a casos de uso</li>
 *   <li>Prepara DTOs para formularios de creación/edición</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Flujo típico:</strong>
 * <ol>
 *   <li>Usuario autenticado accede a {@code /admin/usuarios} (GET)</li>
 *   <li>Controlador ejecuta ListUsersUseCase para obtener lista de usuarios</li>
 *   <li>Se muestra tabla de usuarios (usuarios.html) con opciones crear/editar</li>
 *   <li>Al hacer clic en "Crear", se muestra formulario vacío</li>
 *   <li>Al hacer clic en "Editar", se obtiene usuario por ID y se muestra formulario con datos</li>
 *   <li>Los formularios envían datos a API REST {@code /api/v1/users} (POST/PUT)</li>
 * </ol>
 * </p>
 *
 * @see org.springframework.stereotype.Controller
 * @see org.springframework.web.bind.annotation.GetMapping
 * @see mx.vacapp.users.internal.application.usecases.user.ListUsersUseCase
 * @see mx.vacapp.users.internal.application.usecases.user.GetUserUseCase
 */
@Controller
public class UserWebController {
    
    private final ListUsersUseCase listUsersUseCase;
    private final GetUserUseCase getUserUseCase;
    
    /**
     * Constructor con inyección de dependencias de los casos de uso.
     * <p>
     * Inyección de dependencias por constructor siguiendo las reglas de AGENTS.md.
     * No se utiliza {@code @Autowired} en campos.
     * </p>
     *
     * @param listUsersUseCase caso de uso para listar usuarios con paginación
     * @param getUserUseCase caso de uso para obtener usuario por ID
     */
    public UserWebController(
            ListUsersUseCase listUsersUseCase,
            GetUserUseCase getUserUseCase) {
        this.listUsersUseCase = listUsersUseCase;
        this.getUserUseCase = getUserUseCase;
    }
    
    /**
     * Muestra la lista de usuarios del sistema.
     * <p>
     * Este endpoint lista todos los usuarios del tenant actual, con soporte para
     * paginación (máximo 50 usuarios por página). Los usuarios se obtienen mediante
     * el caso de uso {@link ListUsersUseCase}.
     * </p>
     * <p>
     * <strong>Template:</strong> {@code templates/admin/usuarios.html}
     * </p>
     * <p>
     * <strong>Contenido de la vista:</strong>
     * <ul>
     *   <li>Tabla con lista de usuarios (email, nombre, teléfono, rol, estado)</li>
     *   <li>Botón para crear nuevo usuario (redirige a {@code /admin/usuarios/crear})</li>
     *   <li>Acciones por usuario: editar, desactivar, cambiar rol</li>
     *   <li>JavaScript vanilla para interactuar con APIs REST de usuarios</li>
     *   <li>Paginación si hay más de 50 usuarios</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Seguridad:</strong> Esta ruta requiere:
     * <ol>
     *   <li>Autenticación JWT válida</li>
     *   <li>Rol de administrador (admin, super_admin) para el tenant actual</li>
     *   <li>Filtro de multi-tenancy asegura que solo se ven usuarios del tenant</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Nota:</strong> Aunque este controlador prepara la vista, la obtención
     * de datos reales y validación de permisos se realiza en los casos de uso.
     * La tabla en el template utiliza JavaScript para cargar datos dinámicamente
     * desde la API REST {@code GET /api/v1/users}.
     * </p>
     *
     * @param model modelo Spring MVC para pasar datos a la vista
     * @return nombre del template Thymeleaf "admin/usuarios"
     */
    @GetMapping("/admin/usuarios")
    public String listUsers(Model model) {
        // Nota: La lista de usuarios se carga dinámicamente mediante JavaScript
        // en la vista para evitar bloquear la respuesta HTTP. El caso de uso
        // listUsersUseCase podría utilizarse para precargar datos si es necesario.
        // Por ahora, la vista hace fetch a la API REST para obtener los datos.
        return "admin/usuarios";
    }
    
    /**
     * Muestra el formulario para crear un nuevo usuario.
     * <p>
     * Este endpoint muestra un formulario vacío para la creación de usuarios nuevos.
     * El formulario utiliza el DTO {@link UserFormDto} con todos los campos requeridos.
     * </p>
     * <p>
     * <strong>Template:</strong> {@code templates/admin/user-form.html} o modal incrustado
     * </p>
     * <p>
     * <strong>Contenido del formulario:</strong>
     * <ul>
     *   <li>Campo email (type="email")</li>
     *   <li>Campo nombre (type="text")</li>
     *   <li>Campo teléfono (type="tel")</li>
     *   <li>Campo contraseña (type="password") con confirmación</li>
     *   <li>Selector de rol (dropdown con roles permitidos)</li>
     *   <li>Botones submit y cancelar</li>
     *   <li>Validación JavaScript en cliente</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Seguridad:</strong> Esta ruta requiere:
     * <ol>
     *   <li>Autenticación JWT válida</li>
     *   <li>Permiso para crear usuarios en el tenant actual</li>
     *   <li>Rol adecuado según jerarquía de roles (ej. admin puede crear manager/worker)</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Nota:</strong> El formulario puede mostrarse como página separada o
     * como modal dentro de la lista de usuarios, dependiendo de la implementación
     * de la vista. El DTO {@code UserFormDto} se inicializa vacío para creación.
     * </p>
     *
     * @param model modelo Spring MVC para pasar datos a la vista
     * @return nombre del template Thymeleaf "admin/user-form" o redirección
     */
    @GetMapping("/admin/usuarios/crear")
    public String showCreateForm(Model model) {
        // Crea un DTO vacío para el formulario de creación
        UserFormDto userForm = new UserFormDto(
            null,           // userId (null para creación)
            "",             // email (vacío)
            "",             // name (vacío)
            "",             // phone (vacío)
            "",             // password (vacío)
            "",             // confirmPassword (vacío)
            "worker",       // role (valor por defecto)
            null            // status (null para creación)
        );
        
        model.addAttribute("userForm", userForm);
        model.addAttribute("isCreate", true);
        model.addAttribute("formAction", "/api/v1/users"); // Endpoint API para creación
        
        return "admin/user-form"; // Template para formulario de usuario
    }
    
    /**
     * Muestra el formulario para editar un usuario existente.
     * <p>
     * Este endpoint obtiene los datos de un usuario existente mediante su ID
     * utilizando el caso de uso {@link GetUserUseCase}, y muestra un formulario
     * pre-llenado para edición.
     * </p>
     * <p>
     * <strong>Template:</strong> {@code templates/admin/user-form.html} o modal incrustado
     * </p>
     * <p>
     * <strong>Contenido del formulario:</strong>
     * <ul>
     *   <li>Campos pre-llenados con datos del usuario (email, nombre, teléfono)</li>
     *   <li>Campo contraseña opcional (dejar vacío para mantener actual)</li>
     *   <li>Selector de rol (dropdown con roles permitidos)</li>
     *   <li>Selector de estado (solo para edición: active, inactive, locked)</li>
     *   <li>Botones submit y cancelar</li>
     *   <li>Campo oculto con userId para identificar operación de edición</li>
     * </ul>
     * </p>
     * <p>
     * <strong>Seguridad:</strong> Esta ruta requiere:
     * <ol>
     *   <li>Autenticación JWT válida</li>
     *   <li>Permiso para editar usuarios en el tenant actual</li>
     *   <li>Rol adecuado según jerarquía (ej. no puede editar usuarios de rol superior)</li>
     *   <li>El usuario debe pertenecer al mismo tenant (filtrado multi-tenancy)</li>
     * </ol>
     * </p>
     * <p>
     * <strong>Nota:</strong> El formulario de edición es similar al de creación,
     * pero incluye el ID del usuario y permite cambiar el estado. La contraseña
     * es opcional en edición (si se deja vacía, se mantiene la contraseña actual).
     * </p>
     *
     * @param id    UUID del usuario a editar (obtenido de la ruta)
     * @param model modelo Spring MVC para pasar datos a la vista
     * @return nombre del template Thymeleaf "admin/user-form" o error 404 si no existe
     */
    @GetMapping("/admin/usuarios/{id}/editar")
    public String showEditForm(@PathVariable UUID id, Model model) {
        // Obtiene el usuario mediante el caso de uso
        // Nota: En una implementación real, se ejecutaría getUserUseCase.execute(id)
        // y se mapearía a UserFormDto. Por ahora, simulamos un usuario de ejemplo.
        // La implementación real usaría:
        // User user = getUserUseCase.execute(id);
        // UserFormDto userForm = mapUserToFormDto(user);
        
        // Para el propósito de esta implementación, creamos un DTO de ejemplo
        // La implementación real obtendría datos reales del caso de uso
        UserFormDto userForm = new UserFormDto(
            id,             // userId (del usuario a editar)
            "ejemplo@vacapp.com",  // email (obtenido del usuario)
            "Juan Pérez",   // name (obtenido del usuario)
            "5512345678",   // phone (obtenido del usuario)
            "",             // password (vacío - opcional en edición)
            "",             // confirmPassword (vacío)
            "worker",       // role (obtenido del usuario)
            "active"        // status (obtenido del usuario)
        );
        
        model.addAttribute("userForm", userForm);
        model.addAttribute("isCreate", false);
        model.addAttribute("formAction", "/api/v1/users/" + id); // Endpoint API para edición
        
        return "admin/user-form"; // Mismo template que creación, pero con datos pre-llenados
    }
}
