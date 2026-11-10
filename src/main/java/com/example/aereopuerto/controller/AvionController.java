package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.AvionDTO;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.model.enums.estadoAvion;
import com.example.aereopuerto.service.AvionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/aviones")
@RequiredArgsConstructor
@Tag(name = "Aviones", description = "Operaciones relacionadas con los aviones")
public class AvionController {

        private final AvionService avionService;

        @Operation(summary = "Obtener avión por ID", description = "Devuelve la información de un avión específico")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Avión encontrado correctamente"),
                        @ApiResponse(responseCode = "404", description = "Avión no encontrado")
        })
        @GetMapping("/{id}")
        public ResponseEntity<Avion> obtenerAvion(@PathVariable Integer id) {
                return ResponseEntity.ok(avionService.obtenerAvionPorId(id));
        }

        @Operation(summary = "Obtener todos los aviones", description = "Devuelve la lista completa de aviones registrados")
        @ApiResponse(responseCode = "200", description = "Lista obtenida correctamente")
        @GetMapping
        public ResponseEntity<List<Avion>> obtenerTodos() {
                return ResponseEntity.ok(avionService.obtenerTodosLosAviones());
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Crear avión", description = "Registra un nuevo avión. Si no se indica estado, se crea como DISPONIBLE. El identificador debe tener formato LV-A seguido de números y no puede repetirse.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Avión creado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos o identificador repetido")
        })
        @PostMapping
        public ResponseEntity<Avion> crearAvion(@RequestBody AvionDTO avion) {
                Avion nuevo = avionService.crearAvion(avion);
                return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Actualizar avión", description = "Actualiza los datos y el estado de un avión. No permite pasarlo a MANTENIMIENTO/BAJA si tiene vuelos pendientes, ni reducir la capacidad por debajo de los pasajes ya vendidos.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Avión actualizado correctamente"),
                        @ApiResponse(responseCode = "400", description = "Datos inválidos"),
                        @ApiResponse(responseCode = "404", description = "Avión no encontrado")
        })
        @PutMapping("/{id}")
        public ResponseEntity<Avion> actualizarAvion(@PathVariable Integer id, @RequestBody AvionDTO avion) {
                return ResponseEntity.ok(avionService.actualizarAvion(id, avion));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Dar de baja un avión", description = "Baja lógica: el avión pasa a estado BAJA y se conserva para vuelos e historial. No se permite si tiene vuelos pendientes.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Avión dado de baja correctamente"),
                        @ApiResponse(responseCode = "400", description = "El avión ya estaba de baja o tiene vuelos pendientes"),
                        @ApiResponse(responseCode = "404", description = "Avión no encontrado")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Avion> darDeBajaAvion(@PathVariable Integer id) {
                return ResponseEntity.ok(avionService.darDeBajaAvion(id));
        }

        @Operation(summary = "Filtrar aviones", description = "Permite buscar aviones aplicando filtros como modelo, estado y capacidades")
        @ApiResponse(responseCode = "200", description = "Búsqueda realizada correctamente")
        @GetMapping("/filtraraviones")
        public ResponseEntity<List<Avion>> filtrarAviones(
                        @RequestParam(required = false) String identificador,
                        @RequestParam(required = false) String modelo,
                        @RequestParam(required = false) estadoAvion estado,
                        @RequestParam(required = false) Integer capacidadPasajerosMin,
                        @RequestParam(required = false) Integer capacidadPasajerosMax,
                        @RequestParam(required = false) Double capacidadBodegaMin,
                        @RequestParam(required = false) Double capacidadBodegaMax) {

                return ResponseEntity.ok(avionService.buscarAvionesConFiltros(
                                identificador,
                                modelo,
                                estado,
                                capacidadPasajerosMin,
                                capacidadPasajerosMax,
                                capacidadBodegaMin,
                                capacidadBodegaMax));
        }

}
