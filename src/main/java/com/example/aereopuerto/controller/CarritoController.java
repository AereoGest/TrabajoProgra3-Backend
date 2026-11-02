package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.CarritoDTO;
import com.example.aereopuerto.dto.CarritoItemDTO;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.service.CarritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carrito")
@RequiredArgsConstructor
@Tag(name = "Carritos", description = "Operaciones relacionadas con los carritos")
public class CarritoController {

    private final CarritoService carritoService;

    @Operation(
            summary = "Obtener mi carrito",
            description = "Devuelve el carrito del usuario autenticado"
    )
    @ApiResponse(responseCode = "200", description = "Carrito obtenido correctamente")
    @GetMapping
    public ResponseEntity<CarritoDTO> getMiCarrito(Authentication authentication) {
        return ResponseEntity.ok(carritoService.getCarritoPorToken(authentication.getName()));
    }

    @Operation(
            summary = "Agregar item al carrito",
            description = "Agrega un vuelo al carrito del usuario autenticado"
    )
    @ApiResponse(responseCode = "201", description = "Item agregado correctamente")
    @PostMapping("/items")
    public ResponseEntity<CarritoItemDTO> addItemToCarrito(
            @RequestParam Integer vueloId,
            @RequestParam Integer cantidad,
            @RequestParam ClasesVuelo clase,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(carritoService.addItemPorToken(authentication.getName(), vueloId, cantidad, clase));
    }

    @Operation(
            summary = "Eliminar item del carrito",
            description = "Elimina un item del carrito del usuario autenticado"
    )
    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> removeItemFromCarrito(@PathVariable Integer itemId, Authentication authentication) {
        carritoService.removeItemPorToken(authentication.getName(), itemId);
        return ResponseEntity.noContent().build();
    }


    @Operation(
            summary = "Actualizar cantidad de item",
            description = "Modifica la cantidad de un item en el carrito"
    )
    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CarritoItemDTO> updateItemQuantity(
            @PathVariable Integer itemId,
            @RequestParam Integer cantidad,
            Authentication authentication) {
        return ResponseEntity.ok(carritoService.updateItemQuantityPorToken(authentication.getName(), itemId, cantidad));
    }

    @Operation(
            summary = "Vaciar carrito",
            description = "Elimina todos los items del carrito del usuario"
    )
    @DeleteMapping("/{carritoId}/clear")
    public ResponseEntity<Void> clearCarrito(@PathVariable Integer carritoId, Authentication authentication) {
        carritoService.clearCarritoPorToken(authentication.getName(), carritoId);
        return ResponseEntity.noContent().build();
    }
}
