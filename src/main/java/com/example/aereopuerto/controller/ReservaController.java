package com.example.aereopuerto.controller;

import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.dto.CheckinMostradorRequest;
import com.example.aereopuerto.dto.Checkinresponsedto;
import com.example.aereopuerto.dto.PagoRequest;
import com.example.aereopuerto.dto.PagoResponse;
import com.example.aereopuerto.dto.ReservaCheckinDTO;
import com.example.aereopuerto.dto.ReservaDTO;
import com.example.aereopuerto.dto.ReservaRequest;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.service.CheckinService;
import com.example.aereopuerto.service.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

    private final ReservaService reservaService;
    private final CheckinService checkinService;

    // ===================== Cliente autenticado =====================

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

    @Operation(summary = "Hacer mi check-in",
            description = "Hace el check-in de una reserva confirmada del cliente autenticado. A los pasajes sin asiento se les asigna uno libre al azar.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check-in realizado (o ya estaba hecho)"),
            @ApiResponse(responseCode = "403", description = "La reserva no pertenece al usuario"),
            @ApiResponse(responseCode = "404", description = "La reserva no existe"),
            @ApiResponse(responseCode = "409", description = "Reserva no confirmada, vuelo ya salido o sin asientos libres")
    })
    @PostMapping("/mis-reservas/{id}/check-in")
    public ResponseEntity<Checkinresponsedto> hacerMiCheckIn(
            @PathVariable Integer id,
            @AuthenticationPrincipal User usuarioAutenticado) {
        return ResponseEntity.ok(
                checkinService.hacerCheckIn(id, usuarioAutenticado.getPersona().getId()));
    }

    @Operation(summary = "Cancelar reserva", description = "Cancela una reserva existente.")
    @PatchMapping("/cancelar/{id}")
    public ResponseEntity<String> cancelarReservaDesdeFrontend(
            @PathVariable Integer id,
            @AuthenticationPrincipal User usuarioAutenticado) {
        reservaService.cancelarReservaPorUsuario(id, usuarioAutenticado);
        return ResponseEntity.ok("Reserva cancelada correctamente");
    }

    @Operation(summary = "Filtrar mis reservas", description = "Permite al usuario filtrar sus reservas por estado, fechas, valor y cantidad de pasajes. Todos los filtros son opcionales.")
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

    // ===================== Empleado / Admin =====================

    @Operation(summary = "Obtener todas las reservas", description = "Devuelve la lista completa de reservas.")
    @GetMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<List<Reserva>> obtenerTodas() {
        return ResponseEntity.ok(reservaService.obtenerTodasLasReservas());
    }

    @Operation(summary = "Obtener reserva por ID", description = "Devuelve los detalles de una reserva.")
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Reserva> obtenerReserva(@PathVariable Integer id) {
        return ResponseEntity.ok(reservaService.obtenerReservaPorId(id));
    }

    @Operation(summary = "Actualizar reserva", description = "Actualiza una reserva existente.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Reserva> actualizarReserva(@PathVariable Integer id, @RequestBody ReservaDTO reserva) {
        return ResponseEntity.ok(reservaService.actualizarReserva(id, reserva));
    }

    @Operation(summary = "Eliminar reserva", description = "Elimina una reserva.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Void> eliminarReserva(@PathVariable Integer id) {
        reservaService.eliminarReserva(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Filtrar todas las reservas", description = "Aplica los filtros deseados a la totalidad de las reservas.")
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

    // ===================== Check-in en mostrador (Empleado / Admin) =====================

    @Operation(summary = "Buscar reserva para check-in (mostrador)",
            description = "El empleado o admin busca la reserva con el código de reserva y el documento que le da el pasajero. No modifica nada.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "400", description = "Falta el código o el documento"),
            @ApiResponse(responseCode = "403", description = "El usuario no es empleado ni admin"),
            @ApiResponse(responseCode = "404", description = "No hay una reserva con ese código y documento")
    })
    @PostMapping("/check-in/buscar")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<ReservaCheckinDTO> buscarReservaParaCheckIn(
            @Valid @RequestBody CheckinMostradorRequest request) {
        return ResponseEntity.ok(
                checkinService.buscarParaMostrador(request.getCodigoReserva(), request.getDocumento()));
    }

    @Operation(summary = "Hacer check-in de otra persona (mostrador)",
            description = "El empleado o admin hace el check-in de una reserva confirmada. Exige el código de reserva y el documento del pasajero o del titular.",
            security = @SecurityRequirement(name = "bearerAuth"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Check-in realizado (o ya estaba hecho)"),
            @ApiResponse(responseCode = "400", description = "Falta el código o el documento"),
            @ApiResponse(responseCode = "403", description = "El usuario no es empleado ni admin"),
            @ApiResponse(responseCode = "404", description = "No hay una reserva con ese código y documento"),
            @ApiResponse(responseCode = "409", description = "Reserva no confirmada, vuelo ya salido o sin asientos libres")
    })
    @PostMapping("/check-in/mostrador")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Checkinresponsedto> hacerCheckInMostrador(
            @Valid @RequestBody CheckinMostradorRequest request) {
        return ResponseEntity.ok(
                checkinService.hacerCheckInMostrador(request.getCodigoReserva(), request.getDocumento()));
    }
}