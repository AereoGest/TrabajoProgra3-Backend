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
    private final TotpService totpService;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        boolean email2fa = Boolean.TRUE.equals(user.getTwoFactorEnabled());
        boolean totp2fa = Boolean.TRUE.equals(user.getTotpEnabled());

        if (email2fa || totp2fa) {
            if (email2fa) {
                String code = String.format("%06d", secureRandom.nextInt(1_000_000));
                user.setTwoFactorCode(code);
                user.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(10));
                userRepository.save(user);
                emailService.sendTwoFactorCode(user.getEmail(), code);
            }

            AuthResponse response = new AuthResponse();
            response.setRequires2fa(true);
            response.setEmail(user.getEmail());
            response.setTwoFactorEnabled(email2fa);
            response.setTotpEnabled(totp2fa);
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
                .twoFactorEnabled(user.getTwoFactorEnabled())
                .totpEnabled(user.getTotpEnabled())
                .isVerified(user.getIsVerified())
                .role(user.getRole().name())
                .build();
    }

    public List<UsuarioResponse> obtenerUsuarios() {

        List<User> usuarios = userRepository.findAll();

        return usuarios.stream()
                .map(this::mapearUsuarioResponse)
                .toList();
    }

    public UsuarioResponse obtenerUsuario (Integer id)
    {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Usuario no encontrado"));

        return mapearUsuarioResponse(user);
    }

    public UsuarioResponse mapearUsuarioResponse(User user) {
        Persona persona = user.getPersona();
        return UsuarioResponse.builder()
                .id(user.getId())
                .personaId(persona != null ? persona.getId() : null)
                .nombre(persona != null ? persona.getNombre() : null)
                .apellido(persona != null ? persona.getApellido() : null)
                .email(user.getEmail())
                .telefono(user.getTelefono())
                .role(user.getRole())
                .identificador(persona != null ? persona.getIdentificador() : null)
                .numeroIdentificador(persona != null ? persona.getNumeroIdentificador() : null)
                .fechaNacimiento(persona != null ? persona.getFechaNacimiento() : null)
                .sexo(persona != null ? persona.getSexo() : null)
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

        String inputCode = request.getCode() != null ? request.getCode().trim() : "";
        boolean verified = false;

        // Validar TOTP si está activado
        if (Boolean.TRUE.equals(user.getTotpEnabled()) && user.getTotpSecret() != null) {
            if (totpService.verifyCode(user.getTotpSecret(), inputCode)) {
                verified = true;
            }
        }

        // Si no se validó por TOTP, verificar si coincide con el código de email generado
        if (!verified && user.getTwoFactorCode() != null) {
            if (user.getTwoFactorCodeExpiry() != null
                    && !user.getTwoFactorCodeExpiry().isBefore(LocalDateTime.now())
                    && inputCode.equals(user.getTwoFactorCode().trim())) {
                verified = true;
                user.setTwoFactorCode(null);
                user.setTwoFactorCodeExpiry(null);
                userRepository.save(user);
            }
        }

        if (!verified) {
            throw new IllegalArgumentException("Código de autenticación incorrecto o expirado");
        }

        return buildAuthResponse(user, jwtService.generateToken(user, user.getId()));
    }

    public TotpSetupResponse iniciarSetupTotp() {
        User user = obtenerUsuarioActual();
        String secret = totpService.generateSecret();
        String otpauthUrl = totpService.getOtpAuthUri(secret, user.getEmail(), "AeroGest");
        String qrCodeUrl = totpService.getQrCodeUrl(otpauthUrl);

        return TotpSetupResponse.builder()
                .secret(secret)
                .otpauthUrl(otpauthUrl)
                .qrCodeUrl(qrCodeUrl)
                .build();
    }

    public ResponseEntity<String> activarTotp(TotpVerifyRequest request) {
        User user = obtenerUsuarioActual();
        if (request.getSecret() == null || request.getSecret().isBlank() ||
                request.getCode() == null || request.getCode().isBlank()) {
            throw new IllegalArgumentException("El código y la clave secreta son requeridos");
        }

        boolean valid = totpService.verifyCode(request.getSecret().trim(), request.getCode().trim());
        if (!valid) {
            throw new IllegalArgumentException("El código de verificación es inválido. Asegúrate de ingresar el código actual de 6 dígitos que muestra tu app.");
        }

        user.setTotpSecret(request.getSecret().trim());
        user.setTotpEnabled(true);
        userRepository.save(user);

        return ResponseEntity.ok("Autenticador móvil activado exitosamente.");
    }

    public ResponseEntity<String> desactivarTotp() {
        User user = obtenerUsuarioActual();
        user.setTotpEnabled(false);
        user.setTotpSecret(null);
        userRepository.save(user);

        return ResponseEntity.ok("Autenticador móvil desactivado exitosamente.");
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
