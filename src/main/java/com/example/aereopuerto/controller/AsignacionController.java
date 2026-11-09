package com.example.aereopuerto.controller;


import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.model.Asignacion;
import com.example.aereopuerto.service.AsignacionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.el.parser.AstGreaterThan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.List;

@RestController
@RequestMapping("/api/asignacion")
@RequiredArgsConstructor
@Tag(name = "Asignaciones", description = "Roles de los empleados segun los vuelos")
public class AsignacionController {

    @Autowired
    private AsignacionService asignacionService;

    @Operation(
            summary = "Obtener mis asignaciones",
            description = "Devuelve las asignaciones del empleado autenticado, ordenadas por fecha de salida"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping("/mias")
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
    public ResponseEntity<Asignacion> obtenerAsignacion(@PathVariable Integer id) {
        return ResponseEntity.ok(asignacionService.obtenerAsignacionPorId(id));
    }

    @Operation(
            summary = "Obtener todas las asignaciones",
            description = "Devuelve todas las asignaciones registradas en el sistema"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public ResponseEntity<List<Asignacion>> obtenerTodos() {
        return ResponseEntity.ok(asignacionService.obtenerTodosLasAsignaciones());
    }

    @Operation(
            summary = "Crear asignación",
            description = "Registra una nueva asignación de empleado a vuelo"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Asignación creada correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PostMapping
    public ResponseEntity<Asignacion> crearAsignacion(@RequestBody Asignacion asignacion) {
        Asignacion nuevo = asignacionService.crearAsignacion(asignacion);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Operation(
            summary = "Actualizar asignación",
            description = "Modifica una asignación existente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Asignación actualizada correctamente"),
            @ApiResponse(responseCode = "404", description = "Asignación no encontrada")
    })
    @PutMapping("/{id}")
    public ResponseEntity<Asignacion> actualizarAsignacion(@PathVariable Integer id, @RequestBody Asignacion asignacion) {
        return ResponseEntity.ok(asignacionService.EditarAeropuerto(id, asignacion));
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
    public ResponseEntity<Void> eliminarAsignacion(@PathVariable Integer id) {
        asignacionService.eliminarAsignacion(id);
        return ResponseEntity.noContent().build();
    }




}
