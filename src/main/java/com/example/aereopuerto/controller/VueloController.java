package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.VueloDTO;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.service.VueloService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/vuelos")
@RequiredArgsConstructor
@Tag(name = "Vuelos", description = "Operaciones relacionadas con los vuelos")
public class VueloController {

        @Autowired
        private final VueloService vueloService;

        @Operation(summary = "Obtener un vuelo por su ID", description = "Devuelve los detalles de un vuelo. Utiliza caché Redis para respuestas ultra rapidas.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vuelo encontrado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Vuelo no encontrado")
        })
        @GetMapping("/{id}")
        public ResponseEntity<Vuelo> obtenerVuelo(@PathVariable Integer id) {
                Vuelo vuelo = vueloService.obtenerVueloPorId(id);
                return ResponseEntity.ok(vuelo);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Obtener todos los vuelos", description = "Devuelve una lista de todos los vuelos programados.")
        @ApiResponse(responseCode = "200", description = "Lista devuelta exitosamente")
        @GetMapping
        public ResponseEntity<List<Vuelo>> obtenerTodos() {
                return ResponseEntity.ok(vueloService.obtenerTodosLosVuelos());
        }

        @Operation(summary = "Obtener solo los vuelos disponibles", description = "Devuelve una lista de todos los vuelos disponibles.")
        @ApiResponse(responseCode = "200", description = "Lista devuelta exitosamente")
        @GetMapping("/disponibles")
        public ResponseEntity<List<Vuelo>> obtenerVuelosDisponibles() {
                return ResponseEntity.ok(vueloService.obtenerVuelosCliente());
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Crear un nuevo vuelo", description = "Crea un vuelo en MySQL y actualiza automáticamente la caché de Redis.")
        @ApiResponse(responseCode = "201", description = "Vuelo creado exitosamente")
        @PostMapping
        public ResponseEntity<Vuelo> crearVuelo(@RequestBody Vuelo vuelo) {
                vueloService.invalidarListaDeVuelos();
                Vuelo nuevoVuelo = vueloService.crearVuelo(vuelo);
                return ResponseEntity.status(HttpStatus.CREATED).body(nuevoVuelo);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Actualizar un vuelo", description = "Actualiza los datos de un vuelo en MySQL y refresca la cache de Redis para no perder sincronia.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vuelo actualizado exitosamente"),
                        @ApiResponse(responseCode = "404", description = "Vuelo a actualizar no encontrado")
        })
        @PutMapping("/{id}")
        public ResponseEntity<Vuelo> actualizarVuelo(@PathVariable Integer id, @RequestBody VueloDTO vuelo) {
                Vuelo actualizado = vueloService.actualizarVuelo(id, vuelo);
                return ResponseEntity.ok(actualizado);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Eliminar un vuelo", description = "Borra el vuelo de la base de datos e invalida su entrada en la cache Redis.")
        @ApiResponse(responseCode = "204", description = "Vuelo eliminado exitosamente")
        @DeleteMapping("/{id}")
        public ResponseEntity<Void> eliminarVuelo(@PathVariable Integer id) {
                vueloService.invalidarListaDeVuelos();
                vueloService.eliminarVuelo(id);
                return ResponseEntity.noContent().build();
        }

        @Operation(summary = "Filtrar vuelos disponibles", description = "Filtra el listado de vuelos disponibles según los criterios indicados. Todos los parámetros son opcionales y se combinan entre sí.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vuelos encontrados exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Parámetros de búsqueda inválidos")
        })
        @GetMapping("/disponibles/filtrar")
        public ResponseEntity<List<Vuelo>> buscarVuelosDisponibles(

                        @RequestParam(required = false) String origen,
                        @RequestParam(required = false) String destino,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaSalida,

                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaLlegada,

                        @RequestParam(required = false) Double precioMaximo,
                        @RequestParam(required = false) Boolean escala) {

                return ResponseEntity.ok(
                                vueloService.buscarVuelosCliente(
                                                origen,
                                                destino,
                                                fechaSalida,
                                                fechaLlegada,
                                                precioMaximo,
                                                escala));
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Filtrar vuelos", description = "Filtra el listado de vuelos según los criterios indicados. Todos los parámetros son opcionales y se combinan entre sí.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vuelos encontrados exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Parámetros de búsqueda inválidos")
        })
        @GetMapping("/filtrar")
        public ResponseEntity<List<Vuelo>> buscarVuelos(
                        @Parameter(description = "Ciudad, país o código IATA de origen (búsqueda parcial, sin distinción de mayúsculas)") @RequestParam(required = false) String origen,

                        @Parameter(description = "Ciudad, país o código IATA de destino (búsqueda parcial, sin distinción de mayúsculas)") @RequestParam(required = false) String destino,

                        @Parameter(description = "Fecha/hora mínima de salida (yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaSalida,

                        @Parameter(description = "Fecha/hora máxima de llegada (ISO 8601: yyyy-MM-dd'T'HH:mm:ss)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaLlegada,

                        @Parameter(description = "Precio máximo del vuelo") @RequestParam(required = false) Double precioMaximo,

                        @Parameter(description = "true = con escala, false = vuelo directo") @RequestParam(required = false) Boolean escala,

                        @Parameter(description = "Estado del vuelo: PROGRAMADO, BOARDING, ACTIVO, CANCELADO, REPROGRAMADO") @RequestParam(required = false) estadoVuelo estado) {

                List<Vuelo> vuelos = vueloService.buscarVuelosConFiltrosAvanzados(
                                origen,
                                destino,
                                fechaSalida,
                                fechaLlegada,
                                precioMaximo,
                                escala,
                                estado);

                return ResponseEntity.ok(vuelos);
        }

}