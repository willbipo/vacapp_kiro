package mx.vacapp.geography.internal.infrastructure.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.CreatePotreroCommand;
import mx.vacapp.geography.internal.application.usecases.commands.PotreroResult;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.application.usecases.commands.SeccionResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdatePotreroCommand;
import mx.vacapp.geography.internal.application.usecases.potrero.*;
import mx.vacapp.geography.internal.application.usecases.rancho.ListRanchosUseCase;
import mx.vacapp.geography.internal.application.usecases.seccion.ListSeccionesUseCase;
import mx.vacapp.geography.internal.infrastructure.controllers.web.dtos.PotreroFormDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Controlador web MVC para gestión de potreros con Thymeleaf.
 * 
 * Rutas:
 * - GET  /geography/potreros           → lista de potreros
 * - GET  /geography/potreros/{id}      → detalle de potrero
 * - GET  /geography/potreros/nuevo     → formulario de creación
 * - POST /geography/potreros           → procesar creación
 * - GET  /geography/potreros/{id}/editar → formulario de edición
 * - POST /geography/potreros/{id}      → procesar edición
 */
@Controller
@RequestMapping("/geography/potreros")
@RequiredArgsConstructor
@Slf4j
public class PotreroWebController {
    
    private final CreatePotreroUseCase createPotreroUseCase;
    private final UpdatePotreroUseCase updatePotreroUseCase;
    private final GetPotreroUseCase getPotreroUseCase;
    private final ListPotrerosUseCase listPotrerosUseCase;
    private final ArchivePotreroUseCase archivePotreroUseCase;
    private final ListRanchosUseCase listRanchosUseCase;
    private final ListSeccionesUseCase listSeccionesUseCase;
    
    /**
     * Lista de potreros con filtros opcionales por rancho o sección.
     */
    @GetMapping
    public String listaPotreros(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) UUID ranchoId,
            @RequestParam(required = false) UUID seccionId,
            Model model) {
        
        log.debug("GET /geography/potreros - ranchoId: {}, seccionId: {}, page: {}, size: {}", 
            ranchoId, seccionId, page, size);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        List<PotreroResult> potreros;
        
        if (seccionId != null) {
            // Filtrar por sección específica
            potreros = listPotrerosUseCase.executeBySeccion(seccionId, tenantId);
        } else if (ranchoId != null) {
            // Filtrar por rancho específico
            potreros = listPotrerosUseCase.executeByRancho(ranchoId, tenantId);
        } else {
            // Listar todos los potreros del tenant
            potreros = listPotrerosUseCase.executeAll(tenantId, page, size);
        }
        
        model.addAttribute("potreros", potreros);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("ranchoIdFilter", ranchoId);
        model.addAttribute("seccionIdFilter", seccionId);
        
        return "geography/potreros/lista";
    }
    
    /**
     * Detalle de un potrero.
     * Muestra warning si tiene ganado asignado.
     */
    @GetMapping("/{id}")
    public String detallePotrero(@PathVariable UUID id, Model model) {
        log.debug("GET /geography/potreros/{}", id);
        
        PotreroResult potrero = getPotreroUseCase.execute(id);
        
        model.addAttribute("potrero", potrero);
        
        // Warning si tiene ganado asignado
        if (potrero.cattleCount() > 0) {
            model.addAttribute("cattleWarning", 
                "Este potrero tiene " + potrero.cattleCount() + " animales asignados. " +
                "Traslade el ganado antes de archivar.");
        }
        
        return "geography/potreros/detalle";
    }
    
    /**
     * Formulario de creación de potrero.
     * Carga ranchos y secciones para selectores.
     */
    @GetMapping("/nuevo")
    public String formularioNuevoPotrero(
            @RequestParam(required = false) UUID ranchoId,
            @RequestParam(required = false) UUID seccionId,
            Model model) {
        
        log.debug("GET /geography/potreros/nuevo - ranchoId: {}, seccionId: {}", ranchoId, seccionId);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        // Cargar lista de ranchos
        List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
        
        // Cargar secciones si hay rancho seleccionado
        List<SeccionResult> secciones = List.of();
        if (ranchoId != null) {
            secciones = listSeccionesUseCase.executeByRancho(ranchoId, tenantId);
        }
        
        PotreroFormDto form = new PotreroFormDto();
        if (ranchoId != null) {
            form = new PotreroFormDto(null, ranchoId, seccionId, "", null, "", 0);
        }
        
        model.addAttribute("potreroForm", form);
        model.addAttribute("ranchos", ranchos);
        model.addAttribute("secciones", secciones);
        model.addAttribute("isEdit", false);
        
        return "geography/potreros/formulario";
    }
    
    /**
     * Procesar creación de potrero.
     */
    @PostMapping
    public String crearPotrero(
            @Valid @ModelAttribute("potreroForm") PotreroFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("POST /geography/potreros - nombre: {}, ranchoId: {}, seccionId: {}", 
            form.nombre(), form.ranchoId(), form.seccionId());
        
        if (bindingResult.hasErrors()) {
            // Recargar listas
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            List<SeccionResult> secciones = form.ranchoId() != null 
                ? listSeccionesUseCase.executeByRancho(form.ranchoId(), tenantId) 
                : List.of();
            
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("secciones", secciones);
            model.addAttribute("isEdit", false);
            return "geography/potreros/formulario";
        }
        
        try {
            // TODO: Extraer tenantId y userId del contexto de seguridad
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            
            CreatePotreroCommand command = new CreatePotreroCommand(
                form.nombre(),
                form.superficie(),
                form.ranchoId(),
                form.seccionId(), // Puede ser null
                form.descripcion(),
                tenantId,
                userId
            );
            
            PotreroResult result = createPotreroUseCase.execute(command);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Potrero '" + result.nombre() + "' creado exitosamente");
            
            return "redirect:/geography/potreros/" + result.potreroId();
            
        } catch (Exception e) {
            log.error("Error creando potrero", e);
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            List<SeccionResult> secciones = form.ranchoId() != null 
                ? listSeccionesUseCase.executeByRancho(form.ranchoId(), tenantId) 
                : List.of();
            
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("secciones", secciones);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "geography/potreros/formulario";
        }
    }
    
    /**
     * Formulario de edición de potrero.
     */
    @GetMapping("/{id}/editar")
    public String formularioEditarPotrero(@PathVariable UUID id, Model model) {
        log.debug("GET /geography/potreros/{}/editar", id);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        PotreroResult potrero = getPotreroUseCase.execute(id);
        List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
        List<SeccionResult> secciones = listSeccionesUseCase.executeByRancho(potrero.ranchoId(), tenantId);
        
        PotreroFormDto form = new PotreroFormDto(
            potrero.potreroId(),
            potrero.ranchoId(),
            potrero.seccionId(),
            potrero.nombre(),
            potrero.superficie(),
            potrero.descripcion(),
            potrero.cattleCount()
        );
        
        model.addAttribute("potreroForm", form);
        model.addAttribute("ranchos", ranchos);
        model.addAttribute("secciones", secciones);
        model.addAttribute("isEdit", true);
        
        // Warning si tiene ganado
        if (potrero.cattleCount() > 0) {
            model.addAttribute("cattleWarning", 
                "Este potrero tiene " + potrero.cattleCount() + " animales asignados.");
        }
        
        return "geography/potreros/formulario";
    }
    
    /**
     * Procesar edición de potrero.
     */
    @PostMapping("/{id}")
    public String actualizarPotrero(
            @PathVariable UUID id,
            @Valid @ModelAttribute("potreroForm") PotreroFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("POST /geography/potreros/{} - nombre: {}", id, form.nombre());
        
        if (bindingResult.hasErrors()) {
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            List<SeccionResult> secciones = form.ranchoId() != null 
                ? listSeccionesUseCase.executeByRancho(form.ranchoId(), tenantId) 
                : List.of();
            
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("secciones", secciones);
            model.addAttribute("isEdit", true);
            return "geography/potreros/formulario";
        }
        
        try {
            // TODO: Extraer tenantId y userId del contexto de seguridad
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            
            UpdatePotreroCommand command = new UpdatePotreroCommand(
                id,
                form.nombre(),
                form.superficie(),
                form.descripcion(),
                userId
            );
            
            PotreroResult result = updatePotreroUseCase.execute(command);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Potrero '" + result.nombre() + "' actualizado exitosamente");
            
            return "redirect:/geography/potreros/" + id;
            
        } catch (Exception e) {
            log.error("Error actualizando potrero", e);
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, 0, 100);
            List<SeccionResult> secciones = form.ranchoId() != null 
                ? listSeccionesUseCase.executeByRancho(form.ranchoId(), tenantId) 
                : List.of();
            
            model.addAttribute("ranchos", ranchos);
            model.addAttribute("secciones", secciones);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "geography/potreros/formulario";
        }
    }
}
