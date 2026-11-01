package com.example.aereopuerto.auth.controller;

import com.example.aereopuerto.auth.dto.*;
import com.example.aereopuerto.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "Registro e inicio de sesión de usuarios.")
public class AuthController {

    private final AuthService authService;

    private void setTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("token", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void clearTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("token", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Strict");
        response.addCookie(cookie);
    }

    private void setEmailCookie(HttpServletResponse response, String email) {
        Cookie cookie = new Cookie("user_email", email);
        cookie.setHttpOnly(false);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(24 * 60 * 60);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    private void clearEmailCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("user_email", null);
        cookie.setHttpOnly(false);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        cookie.setAttribute("SameSite", "Lax");
        response.addCookie(cookie);
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica un usuario y devuelve un JWT válido. También setea cookies seguras."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login exitoso"),
            @ApiResponse(responseCode = "400", description = "Credenciales inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        if (authResponse.getToken() != null) {
            setTokenCookie(response, authResponse.getToken());
            setEmailCookie(response, authResponse.getEmail());
        }
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/register")
    @Operation(summary = "Registro", description = "Registra un nuevo usuario vinculado a un Cliente existente.")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @Operation(
            summary = "Obtener perfil",
            description = "Devuelve los datos del usuario autenticado."
    )
    @GetMapping("/perfil")
    public ResponseEntity<PerfilResponse> obtenerPerfil() {
        return ResponseEntity.ok(authService.obtenerPerfil());
    }


    @Operation(
            summary = "Listar usuarios",
            description = "Devuelve todos los usuarios (solo ADMIN)."
    )
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<UsuarioResponse>> obtenerUsuarios() {
        return ResponseEntity.ok(authService.obtenerUsuarios());
    }


    @Operation(
            summary = "Obtener usuario por ID",
            description = "Devuelve un usuario específico (solo ADMIN)."
    )
    @GetMapping("/usuarios/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UsuarioResponse> obtenerUsuario(
            @PathVariable Integer id) {
        return ResponseEntity.ok(authService.obtenerUsuario(id));
    }

    @Operation(
                summary = "Completar perfil",
            description = "Nos permite completar el perfil."
    )
    @PutMapping("/completarPerfil")
    public ResponseEntity<AuthResponse> completarPerfil(@Valid @RequestBody CompletarPerfilRequest request, HttpServletResponse response) {
        ResponseEntity<AuthResponse> res = authService.completarPerfil(request);
        if (res.getBody() != null && res.getBody().getToken() != null) {
            setTokenCookie(response, res.getBody().getToken());
            setEmailCookie(response, res.getBody().getEmail());
        }
        return res;
    }

    @Operation(
            summary = "Actualizar perfil",
            description = "Nos permite actualizar el perfil."
    )
    @PutMapping("/perfil")
    public ResponseEntity<AuthResponse> actualizarPerfil(@Valid @RequestBody CompletarPerfilRequest request, HttpServletResponse response) {
        ResponseEntity<AuthResponse> res = authService.actualizarPerfil(request);
        if (res.getBody() != null && res.getBody().getToken() != null) {
            setTokenCookie(response, res.getBody().getToken());
            setEmailCookie(response, res.getBody().getEmail());
        }
        return res;
    }

    @Operation(
            summary = "Reenviar email de verificación",
            description = "Envía un nuevo enlace de verificación a la casilla del usuario."
    )
    @PostMapping("/resend-verification")
    public ResponseEntity<Map<String, String>> resendVerification(@RequestBody(required = false) Map<String, String> request) {
        String email = request != null ? request.get("email") : null;
        authService.resendVerificationEmail(email);
        return ResponseEntity.ok(Map.of("message", "Si la cuenta existe y no está verificada, se ha enviado un correo de verificación."));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }

    @Operation(
            summary = "Verificar email",
            description = "Verifica el email del usuario mediante token."
    )
    @GetMapping("/verify")
    public ResponseEntity<AuthResponse> verifyEmail(@RequestParam String token, HttpServletResponse response) {
        AuthResponse authResponse = authService.verifyEmail(token);
        if (authResponse.getToken() != null) {
            setTokenCookie(response, authResponse.getToken());
            setEmailCookie(response, authResponse.getEmail());
        }
        return ResponseEntity.ok(authResponse);
    }


    @Operation(
            summary = "Verificar email",
            description = "Verifica el email del usuario mediante token."
    )
    @PostMapping("/verify-2fa")
    public ResponseEntity<AuthResponse> verify2fa(@Valid @RequestBody Verify2FARequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.verify2fa(request);
        if (authResponse.getToken() != null) {
            setTokenCookie(response, authResponse.getToken());
            setEmailCookie(response, authResponse.getEmail());
        }
        return ResponseEntity.ok(authResponse);
    }

    @Operation(
            summary = "Solicitar restablecimiento de contraseña",
            description = "Envía un email con un enlace para restablecer la contraseña."
    )
    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(Map.of("message", "Si el email existe, recibirás un enlace para restablecer tu contraseña."));
    }

    @Operation(
            summary = "Restablecer contraseña",
            description = "Restablece la contraseña usando un token válido."
    )
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(Map.of("message", "Contraseña restablecida exitosamente."));
    }

    @PostMapping("/toggle-2fa")
    public ResponseEntity<String> toggle2fa() {
        return authService.toggle2fa();
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Elimina cookies de autenticación."
    )
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(HttpServletResponse response) {
        clearTokenCookie(response);
        clearEmailCookie(response);
        return ResponseEntity.ok(Map.of("message", "Sesi\u00f3n cerrada exitosamente"));
    }

    /*@PostMapping("/register/limpio")
    @Operation(summary = "Registro", description = "Registra un nuevo usuario vinculado a un Cliente existente.")
    public ResponseEntity<AuthResponse> registerSinPersona(@Valid @RequestBody RegisterRequestSinPersona request) {
        return ResponseEntity.ok(authService.registerSinPersona(request));
    }
     */

}
