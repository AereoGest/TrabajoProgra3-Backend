package com.example.aereopuerto.controller;

import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.dto.PagoRequest;
import com.example.aereopuerto.dto.PagoResponse;
import com.example.aereopuerto.dto.ReservaDTO;
import com.example.aereopuerto.dto.ReservaRequest;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/reservas")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Operaciones relacionadas con las reservas")
public class ReservaController {

    @Autowired
    private final ReservaService reservaService;

    @Operation(summary = "Ver mis reservas", description = "Devuelve todas las reservas del cliente autenticado. Requiere JWT válido.", security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reservas obtenidas exitosamente"),
            @ApiResponse(responseCode = "401", description = "No autenticado")
    })
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<Reserva>> obtenerMisReservas(
            @AuthenticationPrincipal User usuarioAutenticado) {
        return ResponseEntity.ok(reservaService.obtenerMisReservas(usuarioAutenticado));
    }

    @Operation(summary = "Obtener reserva por ID", description = "Devuelve los detalles de una reserva.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Reserva> obtenerReserva(@PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.obtenerReservaPorId(id));
    }

    @Operation(summary = "Obtener todas las reservas", description = "Devuelve la lista completa de reservas.")
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<List<Reserva>> obtenerTodas() {
        return ResponseEntity.ok(reservaService.obtenerTodasLasReservas());
    }

    @Operation(summary = "Crear reserva", description = "Registra una nueva reserva.")
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Reserva> crearReserva(@Valid @RequestBody ReservaRequest reserva) {
        Reserva nueva = reservaService.crearReserva(reserva);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @Operation(summary = "Crear mi reserva", description = "El cliente autenticado crea una reserva a su nombre.")
    @PostMapping("/mis-reservas")
    public ResponseEntity<Reserva> crearMiReserva(
            @AuthenticationPrincipal User usuarioAutenticado,
            @Valid @RequestBody ReservaRequest request) {
        request.setPersonaId(usuarioAutenticado.getPersona().getId());
        Reserva nueva = reservaService.crearReserva(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @Operation(summary = "Pagar mi reserva", description = "Cobra la reserva en Mercado Pago y la confirma si el pago es aprobado.")
    @PostMapping("/mis-reservas/{id}/pagar")
    public ResponseEntity<PagoResponse> pagarMiReserva(
            @PathVariable Integer id,
            @AuthenticationPrincipal User usuarioAutenticado,
            @Valid @RequestBody PagoRequest request) {
        return ResponseEntity.ok(reservaService.pagarReserva(id, request, usuarioAutenticado));
    }

    @Operation(summary = "Actualizar reserva", description = "Actualiza una reserva existente.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Reserva> actualizarReserva(@PathVariable Integer id, @RequestBody ReservaDTO reserva) {
        return ResponseEntity.ok(reservaService.actualizarReserva(id, reserva));
    }

    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva existente.")
    @PutMapping("/{id}/cancelar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<String> cancelarReserva(@PathVariable Integer id) {

        reservaService.cancelarReserva(id);

        return ResponseEntity.ok("Reserva cancelada correctamente");
    }

    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva existente.")
    @PatchMapping("/cancelar/{id}")
    public ResponseEntity<String> cancelarReservaDesdeFrontend(@PathVariable Integer id,
            @AuthenticationPrincipal User usuarioAutenticado) {
        reservaService.cancelarReservaPorUsuario(id, usuarioAutenticado);
        return ResponseEntity.ok("Reserva cancelada correctamente");
    }

    @Operation(summary = "Hacer check-in", description = "Marca una reserva como check-in realizado.")
    @PatchMapping("/{id}/checkin")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<String> hacerCheckin(@PathVariable Integer id) {
        reservaService.hacerCheckin(id);
        return ResponseEntity.ok("Check-in realizado correctamente");
    }

    @Operation(summary = "Eliminar reserva", description = "Elimina una reserva.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Void> eliminarReserva(@PathVariable Integer id) {
        reservaService.eliminarReserva(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Filtrar reservas de su totalidad", description = "Aplica los filtros deseados a la totalidad de las reservas .")
    @GetMapping("/filtrar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<List<Reserva>> filtrarReservas(
            @RequestParam(required = false) String email,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) Double valorMinimo,
            @RequestParam(required = false) Double valorMaximo,
            @RequestParam(required = false) Integer cantidadPasajes) {

        return ResponseEntity.ok(reservaService.buscarTodasReservasConFiltros(
                email, estado, fechaDesde, fechaHasta, valorMinimo, valorMaximo, cantidadPasajes));
    }

    // La idea sería que usuario solo pueda filtrar dentro de SUS reservas
    @Operation(summary = "Filtrar reservas", description = "Permite filtrar reservas por email, estado, fechas, valor y cantidad de pasajes. Todos los filtros son opcionales.")
    @ApiResponse(responseCode = "200", description = "Resultados obtenidos correctamente")
    @GetMapping("/mis-reservas/filtrar")
    public ResponseEntity<List<Reserva>> filtrarMisReservas(
            @AuthenticationPrincipal User usuarioAutenticado,
            @RequestParam(required = false) EstadoReserva estado,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaDesde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaHasta,
            @RequestParam(required = false) Double valorMinimo,
            @RequestParam(required = false) Double valorMaximo,
            @RequestParam(required = false) Integer cantidadPasajes) {

        return ResponseEntity.ok(reservaService.buscarMisReservasConFiltros(
                usuarioAutenticado.getPersona(), estado, fechaDesde, fechaHasta, valorMinimo, valorMaximo,
                cantidadPasajes));
    }

}
