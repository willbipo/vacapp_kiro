package mx.vacapp.geography.internal.infrastructure.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.CreateSeccionCommand;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdateSeccionCommand;
import mx.vacapp.geography.internal.application.usecases.rancho.ListRanchosUseCase;
import mx.vacapp.geography.internal.application.usecases.seccion.*;
import mx.vacapp.geography.internal.infrastructure.controllers.web.dtos.SeccionFormDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Controlador web MVC para gestión de secciones con Thymeleaf.
 * 
 * Rutas:
 * - GET  /geography/secciones           → lista de secciones
 * - GET  /geography/secciones/{id}      → detalle de sección
 * - GET  /geography/secciones/nuevo     → formulario de creación
 * - POST /geography/secciones           → procesar creación
 * - GET  /geography/secciones/{id}/editar → formulario de edición
 * - POST /geography/secciones/{id}      → procesar edición
 */
@Controller
@RequestMapping("/geography/secciones")
@RequiredArgsConstructor
@Slf4j
public class SeccionWebController {
    
    private final CreateSeccionUseCase createSeccionUseCase;
    private final UpdateSeccionUseCase updateSeccionUseCase;
    private final GetSeccionUseCase getSeccionUseCase;
    private final ListSeccionesUseCase listSeccionesUseCase;
    private final ArchiveSeccionUseCase archiveSeccionUseCase;
    private final ListRanchosUseCase listRanchosUseCase;
    
    /**
     * Lista de secciones agrupadas por rancho.
     */
    @GetMapping
    public String listaSecciones(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID ranchoId,
            Model model) {
        
        log.debug("GET /geography/secciones - ranchoId: {}, page: {}, size: {}", ranchoId, page, size);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        List<SeccionResult> secciones;
        
        if (ranchoId != null) {
            // Filtrar por rancho específico
            secciones = listSeccionesUseCase.executeByRancho(ranchoId, tenantId);
        } else {
            // Listar todas las secciones del tenant
            secciones = listSeccionesUseCase.execute(tenantId, page, size);
        }
        
        model.addAttribute("secciones", secciones);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("ranchoIdFilter", ranchoId);
        
        return "geography/secciones/lista";
    }
    
    /**
     * Detalle de una sección.
     */
    @GetMapping("/{id}")
    public String detalleSeccion(@PathVariable UUID id, Model model) {
        log.debug("GET /geography/secciones/{}", id);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        SeccionResult seccion = getSeccionUseCase.execute(id, tenantId);
        
        model.addAttribute("seccion", seccion);
        
        return "geography/secciones/detalle";
    }
    
    /**
     * Formulario de creación de sección.
     */
    @GetMapping("/nuevo")
    public String formularioNuevaSeccion(
            @RequestParam(required = false) UUID ranchoId,
            Model model) {
        
        log.debug("GET /geography/secciones/nuevo - ranchoId: {}", ranchoId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        // Cargar lista de ranchos para el selector
        List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
        
        SeccionFormDto form = new SeccionFormDto();
        if (ranchoId != null) {
            form = new SeccionFormDto(null, ranchoId, "", null, "");
        }
        
        model.addAttribute("seccionForm", form);
        model.addAttribute("ranchos", ranchos);
        model.addAttribute("isEdit", false);
        
        return "geography/secciones/formulario";
    }
    
    /**
     * Procesar creación de sección.
     */
    @PostMapping
    public String crearSeccion(
            @Valid @ModelAttribute("seccionForm") SeccionFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("POST /geography/secciones - nombre: {}, ranchoId: {}", form.nombre(), form.ranchoId());
        
        if (bindingResult.hasErrors()) {
            // Recargar lista de ranchos
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("isEdit", false);
            return "geography/secciones/formulario";
        }
        
        try {
            // TODO: Extraer tenantId y userId del contexto de seguridad
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            
            CreateSeccionCommand command = new CreateSeccionCommand(
                form.nombre(),
                form.superficie(),
                form.ranchoId(),
                form.descripcion(),
                tenantId,
                userId
            );
            
            SeccionResult result = createSeccionUseCase.execute(command);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Sección '" + result.nombre() + "' creada exitosamente");
            
            return "redirect:/geography/secciones/" + result.seccionId();
            
        } catch (Exception e) {
            log.error("Error creando sección", e);
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "geography/secciones/formulario";
        }
    }
    
    /**
     * Formulario de edición de sección.
     */
    @GetMapping("/{id}/editar")
    public String formularioEditarSeccion(@PathVariable UUID id, Model model) {
        log.debug("GET /geography/secciones/{}/editar", id);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        SeccionResult seccion = getSeccionUseCase.execute(id, tenantId);
        List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
        
        SeccionFormDto form = new SeccionFormDto(
            seccion.seccionId(),
            seccion.ranchoId(),
            seccion.nombre(),
            seccion.superficie(),
            seccion.descripcion()
        );
        
        model.addAttribute("seccionForm", form);
        model.addAttribute("ranchos", ranchos);
        model.addAttribute("isEdit", true);
        
        return "geography/secciones/formulario";
    }
    
    /**
     * Procesar edición de sección.
     */
    @PostMapping("/{id}")
    public String actualizarSeccion(
            @PathVariable UUID id,
            @Valid @ModelAttribute("seccionForm") SeccionFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("POST /geography/secciones/{} - nombre: {}", id, form.nombre());
        
        if (bindingResult.hasErrors()) {
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("isEdit", true);
            return "geography/secciones/formulario";
        }
        
        try {
            // TODO: Extraer tenantId y userId del contexto de seguridad
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            
            UpdateSeccionCommand command = new UpdateSeccionCommand(
                id,
                form.nombre(),
                form.superficie(),
                form.descripcion(),
                tenantId,
                userId
            );
            
            SeccionResult result = updateSeccionUseCase.execute(command);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Sección '" + result.nombre() + "' actualizada exitosamente");
            
            return "redirect:/geography/secciones/" + id;
            
        } catch (Exception e) {
            log.error("Error actualizando sección", e);
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "geography/secciones/formulario";
        }
    }
}
