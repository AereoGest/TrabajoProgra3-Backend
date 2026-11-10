package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.ValidacionVueloDTO;
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
        @Operation(summary = "Crear un nuevo vuelo", description = "Da de alta un vuelo a partir de los IDs de aeropuertos y avión. Si no se indica estado, se crea como PROGRAMADO.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "201", description = "Vuelo creado exitosamente"),
                        @ApiResponse(responseCode = "400", description = "Datos del vuelo inválidos o avión no disponible"),
                        @ApiResponse(responseCode = "404", description = "Aeropuerto o avión no encontrado")
        })
        @PostMapping
        public ResponseEntity<Vuelo> crearVuelo(@RequestBody VueloDTO vuelo) {
                Vuelo nuevoVuelo = vueloService.crearVuelo(vuelo);
                return ResponseEntity.status(HttpStatus.CREATED).body(nuevoVuelo);
        }

        @PreAuthorize("hasRole('ADMIN')")
        @Operation(summary = "Validar un vuelo sin guardarlo", description = "Ejecuta las mismas validaciones del alta (sin vueloId) o de la modificación (con vueloId) y devuelve el primer error encontrado. Lo usa el panel de admin para avisar errores mientras se completa el formulario.")
        @ApiResponse(responseCode = "200", description = "Resultado de la validación (valido=true/false y mensaje)")
        @PostMapping("/validar")
        public ResponseEntity<ValidacionVueloDTO> validarVuelo(
                        @Parameter(description = "ID del vuelo que se está modificando. Vacío si es un alta.") @RequestParam(required = false) Integer vueloId,
                        @RequestBody VueloDTO vuelo) {
                return ResponseEntity.ok(vueloService.validarVuelo(vueloId, vuelo));
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
        @Operation(summary = "Dar de baja un vuelo", description = "Baja lógica: el vuelo pasa a estado CANCELADO y deja de mostrarse a los clientes, pero se conserva para reservas e historial.")
        @ApiResponses(value = {
                        @ApiResponse(responseCode = "200", description = "Vuelo dado de baja exitosamente"),
                        @ApiResponse(responseCode = "400", description = "El vuelo no existe o ya estaba cancelado")
        })
        @DeleteMapping("/{id}")
        public ResponseEntity<Vuelo> darDeBajaVuelo(@PathVariable Integer id) {
                return ResponseEntity.ok(vueloService.darDeBajaVuelo(id));
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