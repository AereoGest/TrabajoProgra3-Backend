package com.example.aereopuerto.controller;

import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.dto.CompraDTO;
import com.example.aereopuerto.dto.ProcesarPagoDTO;
import com.example.aereopuerto.model.enums.MetodosDePago;
import com.example.aereopuerto.service.CompraService;
import com.example.aereopuerto.service.MercadoPagoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mercadopago")
@RequiredArgsConstructor
@Tag(name = "Mercado Pago", description = "Operaciones relacionadas con Mercado Pago")
public class MercadoPagoController {

    private final MercadoPagoService mpService;
    private final CompraService compraService;

    @Value("${mercadopago.public-key:APP_USR-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}")
    private String publicKey;

    @Operation(summary = "Obtener public key de MercadoPago")
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }

    @Operation(summary = "Crear preferencia de pago")
    @PostMapping("/crear-preferencia")
    public ResponseEntity<Map<String, Object>> crearPreferencia(
            @AuthenticationPrincipal User user,
            HttpServletRequest request) {
        Double total = compraService.calcularTotalCarrito(user.getPersona().getId());
        String origin = request.getHeader("Origin");
        if (origin == null || origin.isBlank()) {
            origin = "http://localhost:5173";
        }
        var pref = mpService.crearPreferencia(
                total,
                "Compra AeroGest",
                null,
                user,
                String.valueOf(user.getId()),
                origin + "/pago.html");
        String preferenceId = (String) pref.get("id");
        return ResponseEntity.ok(Map.of(
                "preferenceId", preferenceId,
                "publicKey", publicKey));
    }

    @PostMapping("/webhook")
    public ResponseEntity<String> webhook(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        if ("payment".equals(type)) {
            Map<String, Object> data = (Map<String, Object>) body.get("data");
            String paymentId = data != null ? String.valueOf(data.get("id")) : null;
            if (paymentId != null) {
                Map<String, Object> payment = mpService.obtenerPago(paymentId);
                String status = (String) payment.get("status");
                if ("approved".equals(status)) {
                    String externalReference = (String) payment.get("external_reference");
                    compraService.confirmarPagoMP(externalReference, paymentId);
                }
            }
        }
        return ResponseEntity.ok("OK");
    }
}
