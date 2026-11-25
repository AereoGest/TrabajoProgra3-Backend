package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.AeropuertoDTO;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.service.AeropuertoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aeropuertos")
@RequiredArgsConstructor
@Tag(name = "Aeropuertos", description = "Operaciones relacionadas con los aeropuertos")
public class AeropuertoController {

    @Autowired
    private final AeropuertoService aeropuertoService;

    @Operation(
            summary = "Obtener aeropuerto por ID",
            description = "Devuelve la información de un aeropuerto específico según su identificador"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aeropuerto encontrado correctamente"),
            @ApiResponse(responseCode = "404", description = "Aeropuerto no encontrado")
    })
    @GetMapping("/{id}")
    public ResponseEntity<Aeropuerto> obtenerAeropuerto(@PathVariable Integer id) {
        return ResponseEntity.ok(aeropuertoService.obtenerAeropuertoPorId(id));
    }


    @Operation(
            summary = "Obtener todos los aeropuertos",
            description = "Devuelve una lista completa de todos los aeropuertos registrados"
    )
    @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
    @GetMapping
    public ResponseEntity<List<Aeropuerto>> obtenerTodos() {
        return ResponseEntity.ok(aeropuertoService.obtenerTodosLosAeropuertos());
    }

    @Operation(
            summary = "Crear aeropuerto",
            description = "Registra un nuevo aeropuerto en el sistema"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Aeropuerto creado correctamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<Aeropuerto> crearAeropuerto(@Valid @RequestBody AeropuertoDTO aeropuerto) {
        Aeropuerto nuevo = aeropuertoService.crearAeropuerto(aeropuerto);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Operation(
            summary = "Actualizar aeropuerto",
            description = "Actualiza los datos de un aeropuerto existente"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Aeropuerto actualizado correctamente"),
            @ApiResponse(responseCode = "404", description = "Aeropuerto no encontrado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<Aeropuerto> actualizarAeropuerto(
            @PathVariable Integer id,
            @Valid @RequestBody AeropuertoDTO aeropuerto) {
        return ResponseEntity.ok(aeropuertoService.EditarAeropuerto(id, aeropuerto));
    }

    @Operation(
            summary = "Eliminar aeropuerto",
            description = "Elimina un aeropuerto del sistema por su ID"
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Aeropuerto eliminado correctamente"),
            @ApiResponse(responseCode = "404", description = "Aeropuerto no encontrado")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarAeropuerto(@PathVariable Integer id) {
       aeropuertoService.eliminarAeropuerto(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(
            summary = "Filtrar aeropuertos",
            description = "Permite buscar aeropuertos aplicando filtros opcionales como nombre, IATA, ciudad o país"
    )
    @ApiResponse(responseCode = "200", description = "Búsqueda realizada correctamente")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    @GetMapping("/filtrar")
    public ResponseEntity<List<Aeropuerto>> buscarAeropuertos(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String codigoIata,
            @RequestParam(required = false) String ciudad,
            @RequestParam(required = false) String pais) {

        return ResponseEntity.ok(
                aeropuertoService.buscarAeropuertosConFiltros(
                        nombre,
                        codigoIata,
                        ciudad,
                        pais
                )
        );
    }

}
