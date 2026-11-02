package com.example.aereopuerto.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/mercadopago")
@Tag(name = "Mercado Pago", description = "Operaciones relacionadas con Mercado Pago")
public class MercadoPagoController {

    @Value("${mercadopago.public-key:APP_USR-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx}")
    private String publicKey;

    @Operation(summary = "Obtener public key de MercadoPago")
    @GetMapping("/public-key")
    public ResponseEntity<Map<String, String>> getPublicKey() {
        return ResponseEntity.ok(Map.of("publicKey", publicKey));
    }
}
