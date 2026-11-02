package com.example.aereopuerto.controller;

import com.example.aereopuerto.dto.FavoritoDTO;
import com.example.aereopuerto.service.FavoritoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favoritos")
@RequiredArgsConstructor
@Tag(name = "Favoritos", description = "Operaciones relacionadas con los favoritos")
public class FavoritoController {

    private final FavoritoService favoritoService;

    @Operation(summary = "Eliminar un favorito.", description = "Elimina un favorito de una persona.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> removeFavorito(@PathVariable Integer id, Authentication authentication) {
        favoritoService.removeFavoritoPorToken(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Agregar favorito a lista.", description = "Agrega un favorito a lista de la persona.")
    @PostMapping("/vuelos/{vueloId}")
    public ResponseEntity<FavoritoDTO> addFavorito(
            @PathVariable Integer vueloId,
            Authentication authentication) {
        String emailUsuario = authentication.getName();
        FavoritoDTO nuevoFavorito = favoritoService.addFavoritoPorToken(emailUsuario, vueloId);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoFavorito);
    }

    @Operation(summary = "Obtener lista de favoritos por token de persona.", description = "Obtener lista de favoritos por token.")
    @GetMapping
    public ResponseEntity<List<FavoritoDTO>> getMisFavoritos(Authentication authentication) {

        String emailUsuario = authentication.getName();

        List<FavoritoDTO> favoritos = favoritoService.getFavoritosPorToken(emailUsuario);

        return ResponseEntity.ok(favoritos);
    }
}
