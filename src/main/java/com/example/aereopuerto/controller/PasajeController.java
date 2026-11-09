package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.PasajeDTO;
import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.service.PasajeService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/pasajes")
@RequiredArgsConstructor
@Tag(name = "Pasajes", description = "Operaciones relacionadas con los pasajes")
public class PasajeController {

    @Autowired
    private final PasajeService pasajeService;

    @Operation(summary = "Obtener pasaje por ID", description = "Devuelve los datos de un pasaje.")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pasaje> obtenerPasaje(@PathVariable Integer id) {
        return ResponseEntity.ok(pasajeService.obtenerPasajePorId(id));
    }

    @Operation(summary = "Obtener todos los pasajes", description = "Devuelve todos los pasajes registrados.")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Pasaje>> obtenerTodas() {
        return ResponseEntity.ok(pasajeService.obtenerTodasLosPasajes());
    }

    @Operation(summary = "Obtener pasajes de una reserva", description = "Devuelve los pasajes asociados a una reserva específica.")
    @GetMapping("/reserva/{reservaId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Pasaje>> obtenerPorReserva(@PathVariable Integer reservaId) {
        return ResponseEntity.ok(pasajeService.obtenerPasajesPorReserva(reservaId));
    }

    @Operation(summary = "Crear pasaje", description = "Registra un nuevo pasaje.")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pasaje> crearPasaje(@RequestBody Pasaje pasaje) {
        Pasaje nuevo = pasajeService.crearPasaje(pasaje);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevo);
    }

    @Operation(summary = "Actualizar pasaje", description = "Actualiza un pasaje existente asociado a una reserva.")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Pasaje> actualizarPasaje(@PathVariable Integer id, @Valid @RequestBody PasajeDTO pasajeDTO) {
        return ResponseEntity.ok(pasajeService.editarPasaje(id, pasajeDTO));
    }

    @Operation(summary = "Cancelar / eliminar pasaje", description = "Cancela y elimina un pasaje asociado a una reserva, liberando su asiento y recalculando la reserva.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> eliminarPasaje(
            @PathVariable Integer id,
            @RequestParam(required = false) Integer reservaId) {
        pasajeService.cancelarPasajeDeReserva(id, reservaId);
        return ResponseEntity.noContent().build();
    }
}
