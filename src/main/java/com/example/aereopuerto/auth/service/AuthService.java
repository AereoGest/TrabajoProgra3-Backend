package com.example.aereopuerto.auth.service;

import com.example.aereopuerto.auth.dto.*;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.repository.PersonaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.security.SecureRandom;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PersonaRepository personaRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(user.getTwoFactorEnabled())) {
            String code = String.format("%06d", secureRandom.nextInt(1_000_000));
            user.setTwoFactorCode(code);
            user.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(10));
            userRepository.save(user);
            emailService.sendTwoFactorCode(user.getEmail(), code);
            
            AuthResponse response = new AuthResponse();
            response.setRequires2fa(true);
            response.setEmail(user.getEmail());
            return response;
        }

        return buildAuthResponse(user, jwtService.generateToken(user, user.getId()));
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }

        Persona persona = new Persona();
        persona.setApellido("Pendiente");
        persona.setSexo(null);
        persona.setIdentificador(null);
        persona.setNombre("Pendiente");
        persona.setNumeroIdentificador(null);
        persona.setFechaNacimiento(null);
        personaRepository.save(persona);

        String verificationToken = UUID.randomUUID().toString();

        User user = User.builder()
                .persona(persona)
                .email(request.getEmail())
                .telefono("Pendiente")
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_INCOMPLETO)
                .perfilCompleto(false)
                .isVerified(false)
                .verificationToken(verificationToken)
                .twoFactorEnabled(false)
                .build();

        userRepository.save(user);

        emailService.sendVerificationEmail(user.getEmail(), verificationToken);

        return buildAuthResponse(user, null);
    }

    public PerfilResponse obtenerPerfil() {

        User user = obtenerUsuarioActual();

        Persona persona = user.getPersona();

        return PerfilResponse.builder()
                .personaId(persona.getId())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .email(user.getEmail())
                .telefono(user.getTelefono())
                .numeroIdentificador(persona.getNumeroIdentificador())
                .identificador(persona.getIdentificador())
                .fechaNacimiento(persona.getFechaNacimiento())
                .sexo(persona.getSexo())
                .isVerified(user.getIsVerified())
                .role(user.getRole().name())
                .build();
    }

    public List<UsuarioResponse> obtenerUsuarios() {

        List<User> usuarios = userRepository.findAll();

        return usuarios.stream()
                .map(user -> UsuarioResponse.builder()
                        .id(user.getId())
                        .nombre(user.getPersona().getNombre())
                        .apellido(user.getPersona().getApellido())
                        .email(user.getEmail())
                        .telefono(user.getTelefono())
                        .role(user.getRole())
                        .build())
                .toList();
    }

    public UsuarioResponse obtenerUsuario (Integer id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado"));

        Persona persona = user.getPersona();

        return UsuarioResponse.builder()
                .id(user.getId())
                .nombre(persona.getNombre())
                .apellido(persona.getApellido())
                .email(user.getEmail())
                .telefono(user.getTelefono())
                .role(user.getRole())
                .build();
    }


    public ResponseEntity<AuthResponse> completarPerfil(CompletarPerfilRequest request) {
        return guardarPerfil(request);
    }

    public ResponseEntity<AuthResponse> actualizarPerfil(CompletarPerfilRequest request) {
        return guardarPerfil(request);
    }

    private ResponseEntity<AuthResponse> guardarPerfil(CompletarPerfilRequest request) {

        User user = obtenerUsuarioActual();

        validarPerfilRequest(request);

        Persona persona = user.getPersona();

        personaRepository.findByNumeroIdentificador(request.getNumeroIdentificador())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(persona.getId())) {
                        throw new IllegalArgumentException("El número de documento ya está registrado por otro usuario.");
                    }
                });

        persona.setNombre(request.getNombre());
        persona.setApellido(request.getApellido());
        persona.setNumeroIdentificador(request.getNumeroIdentificador());
        persona.setIdentificador(request.getIdentificador());
        persona.setSexo(request.getSexo());
        persona.setFechaNacimiento(request.getFechaNacimiento());
        user.setTelefono(request.getTelefono());

        user.setPerfilCompleto(true);
        if (user.getRole() == Role.ROLE_INCOMPLETO) {
            user.setRole(Role.ROLE_USUARIO);
        }

        personaRepository.save(persona);
        userRepository.save(user);

        return ResponseEntity.ok(buildAuthResponse(user, jwtService.generateToken(user, user.getId())));
    }

    private void validarPerfilRequest(CompletarPerfilRequest request) {
        if (request.getNombre() == null || request.getNombre().isBlank() ||
                request.getApellido() == null || request.getApellido().isBlank() ||
                request.getNumeroIdentificador() == null || request.getNumeroIdentificador().isBlank() ||
                request.getIdentificador() == null ||
                request.getSexo() == null ||
                request.getFechaNacimiento() == null ||
                request.getTelefono() == null || request.getTelefono().isBlank()) {

            throw new IllegalArgumentException(
                    "Todos los campos son obligatorios"
            );
        }
    }

    public AuthResponse verifyEmail(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Token de verificación inválido o ausente");
        }
        User user = userRepository.findByVerificationToken(token.trim())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido"));

        user.setIsVerified(true);
        user.setVerificationToken(null);
        userRepository.save(user);

        return buildAuthResponse(user, jwtService.generateToken(user, user.getId()));
    }

    public AuthResponse verify2fa(Verify2FARequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (user.getTwoFactorCode() == null || user.getTwoFactorCodeExpiry() == null || user.getTwoFactorCodeExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El código 2FA ha expirado o no es válido. Vuelve a iniciar sesión.");
        }
        if (!request.getCode().trim().equals(user.getTwoFactorCode())) {
            throw new IllegalArgumentException("Código 2FA incorrecto");
        }

        user.setTwoFactorCode(null);
        user.setTwoFactorCodeExpiry(null);
        userRepository.save(user);

        return buildAuthResponse(user, jwtService.generateToken(user, user.getId()));
    }

    public void resendVerificationEmail(String email) {
        String targetEmail = email;
        if (targetEmail == null || targetEmail.isBlank()) {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
                targetEmail = auth.getName();
            }
        }
        if (targetEmail == null || targetEmail.isBlank()) {
            throw new IllegalArgumentException("El correo electrónico es obligatorio");
        }

        User user = userRepository.findByEmail(targetEmail.trim())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        if (Boolean.TRUE.equals(user.getIsVerified())) {
            throw new IllegalArgumentException("La cuenta ya se encuentra verificada.");
        }
        String newToken = UUID.randomUUID().toString();
        user.setVerificationToken(newToken);
        userRepository.save(user);
        emailService.sendVerificationEmail(user.getEmail(), newToken);
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            String resetToken = UUID.randomUUID().toString();
            user.setResetToken(resetToken);
            user.setResetTokenExpiry(LocalDateTime.now().plusHours(1));
            userRepository.save(user);

            emailService.sendPasswordResetEmail(user.getEmail(), resetToken);
        });
    }

    public void resetPassword(ResetPasswordRequest request) {
        if (request == null || request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new IllegalArgumentException("Token de restablecimiento inválido o ausente");
        }
        if (request.getNewPassword() == null || request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres");
        }
        User user = userRepository.findByResetToken(request.getToken().trim())
                .orElseThrow(() -> new IllegalArgumentException("Token inválido o expirado"));

        if (user.getResetTokenExpiry() == null || user.getResetTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("El token ha expirado. Solicita un nuevo restablecimiento.");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setResetToken(null);
        user.setResetTokenExpiry(null);
        userRepository.save(user);
    }

    public ResponseEntity<String> toggle2fa() {
        User user = obtenerUsuarioActual();

        boolean currentStatus = Boolean.TRUE.equals(user.getTwoFactorEnabled());
        user.setTwoFactorEnabled(!currentStatus);
        userRepository.save(user);

        String status = !currentStatus ? "habilitado" : "deshabilitado";
        return ResponseEntity.ok("2FA " + status + " exitosamente.");
    }

    private User obtenerUsuarioActual() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
    }

    private AuthResponse buildAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .email(user.getEmail())
                .userId(user.getId())
                .perfilCompleto(user.getPerfilCompleto())
                .requires2fa(false)
                .role(user.getRole() != null ? user.getRole().name() : null)
                .isVerified(user.getIsVerified())
                .build();
    }
}
