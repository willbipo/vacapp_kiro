package mx.vacapp.geography.internal.infrastructure.controllers.web;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import mx.vacapp.geography.internal.application.usecases.commands.CreateRanchoCommand;
import mx.vacapp.geography.internal.application.usecases.commands.RanchoResult;
import mx.vacapp.geography.internal.application.usecases.commands.UpdateRanchoCommand;
import mx.vacapp.geography.internal.application.usecases.rancho.*;
import mx.vacapp.geography.internal.infrastructure.controllers.web.dtos.RanchoFormDto;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.UUID;

/**
 * Controlador web MVC para gestión de ranchos con Thymeleaf.
 * 
 * Rutas:
 * - GET  /geography/ranchos          → lista de ranchos
 * - GET  /geography/ranchos/{id}     → detalle de rancho
 * - GET  /geography/ranchos/nuevo    → formulario de creación
 * - POST /geography/ranchos          → procesar creación
 * - GET  /geography/ranchos/{id}/editar → formulario de edición
 * - POST /geography/ranchos/{id}     → procesar edición
 */
@Controller
@RequestMapping("/geography/ranchos")
@RequiredArgsConstructor
@Slf4j
public class RanchoWebController {
    
    private final CreateRanchoUseCase createRanchoUseCase;
    private final UpdateRanchoUseCase updateRanchoUseCase;
    private final GetRanchoUseCase getRanchoUseCase;
    private final ListRanchosUseCase listRanchosUseCase;
    private final ArchiveRanchoUseCase archiveRanchoUseCase;
    private final GetRanchoStatsUseCase getRanchoStatsUseCase;
    
    /**
     * Lista de ranchos con paginación.
     */
    @GetMapping
    public String listaRanchos(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            Model model) {
        
        log.debug("GET /geography/ranchos - page: {}, size: {}", page, size);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        List<RanchoResult> ranchos = listRanchosUseCase.execute(tenantId, page, size);
        long total = listRanchosUseCase.countByTenant(tenantId);
        
        model.addAttribute("ranchos", ranchos);
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("totalItems", total);
        model.addAttribute("totalPages", (int) Math.ceil((double) total / size));
        
        return "geography/ranchos/lista";
    }
    
    /**
     * Detalle de un rancho con tabs (info, secciones, potreros, estadísticas).
     */
    @GetMapping("/{id}")
    public String detalleRancho(@PathVariable UUID id, Model model) {
        log.debug("GET /geography/ranchos/{}", id);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001"); // Mock temporal
        
        RanchoResult rancho = getRanchoUseCase.execute(id);
        
        model.addAttribute("rancho", rancho);
        
        return "geography/ranchos/detalle";
    }
    
    /**
     * Formulario de creación de rancho.
     */
    @GetMapping("/nuevo")
    public String formularioNuevoRancho(Model model) {
        log.debug("GET /geography/ranchos/nuevo");
        
        model.addAttribute("ranchoForm", new RanchoFormDto());
        model.addAttribute("isEdit", false);
        
        return "geography/ranchos/formulario";
    }
    
    /**
     * Procesar creación de rancho.
     */
    @PostMapping
    public String crearRancho(
            @Valid @ModelAttribute("ranchoForm") RanchoFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("POST /geography/ranchos - nombre: {}", form.nombre());
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "geography/ranchos/formulario";
        }
        
        try {
            // TODO: Extraer tenantId y userId del contexto de seguridad
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            
            CreateRanchoCommand command = new CreateRanchoCommand(
                form.nombre(),
                form.superficieTotal(),
                form.descripcion(),
                tenantId,
                userId
            );
            
            RanchoResult result = createRanchoUseCase.execute(command);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rancho '" + result.nombre() + "' creado exitosamente");
            
            return "redirect:/geography/ranchos/" + result.ranchoId();
            
        } catch (Exception e) {
            log.error("Error creando rancho", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", false);
            return "geography/ranchos/formulario";
        }
    }
    
    /**
     * Formulario de edición de rancho.
     */
    @GetMapping("/{id}/editar")
    public String formularioEditarRancho(@PathVariable UUID id, Model model) {
        log.debug("GET /geography/ranchos/{}/editar", id);
        
        // TODO: Extraer tenantId del contexto de seguridad
        UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        
        RanchoResult rancho = getRanchoUseCase.execute(id);
        
        RanchoFormDto form = new RanchoFormDto(
            rancho.ranchoId(),
            rancho.nombre(),
            rancho.superficieTotal(),
            rancho.descripcion()
        );
        
        model.addAttribute("ranchoForm", form);
        model.addAttribute("isEdit", true);
        
        return "geography/ranchos/formulario";
    }
    
    /**
     * Procesar edición de rancho.
     */
    @PostMapping("/{id}")
    public String actualizarRancho(
            @PathVariable UUID id,
            @Valid @ModelAttribute("ranchoForm") RanchoFormDto form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes) {
        
        log.debug("POST /geography/ranchos/{} - nombre: {}", id, form.nombre());
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            return "geography/ranchos/formulario";
        }
        
        try {
            // TODO: Extraer tenantId y userId del contexto de seguridad
            UUID tenantId = UUID.fromString("00000000-0000-0000-0000-000000000001");
            UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000002");
            
            UpdateRanchoCommand command = new UpdateRanchoCommand(
                id,
                form.nombre(),
                form.superficieTotal(),
                form.descripcion(),
                userId
            );
            
            RanchoResult result = updateRanchoUseCase.execute(command);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Rancho '" + result.nombre() + "' actualizado exitosamente");
            
            return "redirect:/geography/ranchos/" + id;
            
        } catch (Exception e) {
            log.error("Error actualizando rancho", e);
            model.addAttribute("errorMessage", e.getMessage());
            model.addAttribute("isEdit", true);
            return "geography/ranchos/formulario";
        }
    }
}
