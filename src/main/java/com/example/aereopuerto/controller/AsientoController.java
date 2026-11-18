package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.AsientoDTO;
import com.example.aereopuerto.service.AsientoService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/asientos")
@RequiredArgsConstructor
@Tag(name = "Asientos", description = "Operaciones relacionadas con los asientos")
public class AsientoController {

    private final AsientoService asientoService;

    @GetMapping("/avion/{avionId}")
    public ResponseEntity<List<AsientoDTO>> getAsientosPorAvion(@PathVariable Integer avionId) {
        return ResponseEntity.ok(asientoService.obtenerAsientosPorAvion(avionId));
    }

    @GetMapping("/vuelo/{vueloId}")
    public ResponseEntity<List<AsientoDTO>> getAsientosPorVuelo(@PathVariable Integer vueloId) {
        return ResponseEntity.ok(asientoService.obtenerAsientosPorVuelo(vueloId));
    }

    @PostMapping("/verificar")
    public ResponseEntity<Map<Integer, Boolean>> verificarAsientos(@RequestBody List<Integer> ids) {
        return ResponseEntity.ok(asientoService.verificarDisponibilidad(ids));
    }
}
