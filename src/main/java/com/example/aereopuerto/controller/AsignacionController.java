package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.AsignacionDTO;
import com.example.aereopuerto.model.Asignacion;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.service.AsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/asignacion")
@RequiredArgsConstructor
@Tag(name = "Asignaciones", description = "Roles de los empleados segun los vuelos")
public class AsignacionController {

    private final AsignacionService asignacionService;

    @Operation(
            summary = "Obtener mis asignaciones",
            description = "Devuelve las asignaciones del empleado autenticado, ordenadas por fecha de salida"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/mias")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<List<Asignacion>> obtenerMisAsignaciones(Authentication authentication) {
        return ResponseEntity.ok(asignacionService.obtenerMisAsignaciones(authentication.getName()));
    }

    @Operation(
            summary = "Obtener asignación por ID",
            description = "Devuelve una asignación específica según su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asignación encontrada correctamente"),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Asignacion> obtenerAsignacion(@PathVariable Integer id) {
        return ResponseEntity.ok(asignacionService.obtenerAsignacionPorId(id));
    }

    @Operation(
            summary = "Obtener todas las asignaciones",
            description = "Devuelve todas las asignaciones registradas en el sistema"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Asignacion>> obtenerTodos() {
        return ResponseEntity.ok(asignacionService.obtenerTodosLasAsignaciones());
    }

    @Operation(
            summary = "Obtener empleados disponibles",
            description = "Devuelve las personas habilitadas como empleados o administradores para ser asignadas a vuelos"
    )
    @ApiResponse(responseCode = "200", description = "Lista de empleados obtenida correctamente")
    @GetMapping("/empleados")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Persona>> obtenerEmpleadosDisponibles() {
        return ResponseEntity.ok(asignacionService.obtenerEmpleadosDisponibles());
    }

    @Operation(
            summary = "Crear asignación",
            description = "Registra una nueva asignación de empleado a vuelo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asignación creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos o vuelo finalizado/cancelado"),
            @ApiResponse(responseCode = "409", description = "Conflicto por duplicado o superposición horaria")
    })
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Asignacion> crearAsignacion(@Valid @RequestBody AsignacionDTO asignacion) {
        Asignacion nuevo = asignacionService.crearAsignacion(asignacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Operation(
            summary = "Actualizar asignación",
            description = "Modifica una asignación existente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asignación actualizada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos"),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada"),
            @ApiResponse(responseCode = "409", description = "Conflicto por duplicado o superposición horaria")
    })
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Asignacion> actualizarAsignacion(@PathVariable Integer id, @Valid @RequestBody AsignacionDTO asignacion) {
        return ResponseEntity.ok(asignacionService.actualizarAsignacion(id, asignacion));
    }

    @Operation(
            summary = "Eliminar asignación",
            description = "Elimina una asignación del sistema por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Asignación eliminada correctamente"),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable Integer id) {
        asignacionService.eliminarAsignacion(id);
        return ResponseEntity.noContent().build();
    }
}
