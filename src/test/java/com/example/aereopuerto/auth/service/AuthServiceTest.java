package com.example.aereopuerto.auth.service;

import com.example.aereopuerto.auth.dto.AuthResponse;
import com.example.aereopuerto.auth.dto.LoginRequest;
import com.example.aereopuerto.auth.dto.ResetPasswordRequest;
import com.example.aereopuerto.auth.dto.Verify2FARequest;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.repository.PersonaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private Persona testPersona;

    @BeforeEach
    void setUp() {
        testPersona = new Persona();
        testPersona.setId(1);
        testPersona.setNombre("Juan");
        testPersona.setApellido("Perez");

        testUser = User.builder()
                .id(1)
                .email("test@aerogest.com")
                .password("encodedPassword")
                .persona(testPersona)
                .telefono("12345678")
                .role(Role.ROLE_USUARIO)
                .perfilCompleto(true)
                .isVerified(true)
                .twoFactorEnabled(false)
                .build();
    }

    @Test
    @DisplayName("Login exitoso devuelve token, rol y estado de verificación")
    void login_Exitoso() {
        LoginRequest request = new LoginRequest("test@aerogest.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser, testUser.getId())).thenReturn("mock-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock-jwt-token", response.getToken());
        assertEquals("test@aerogest.com", response.getEmail());
        assertEquals(1, response.getUserId());
        assertEquals("ROLE_USUARIO", response.getRole());
        assertTrue(response.getIsVerified());
        assertFalse(response.getRequires2fa());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Login con 2FA habilitado genera código con expiración y no emite JWT inmediato")
    void login_Con2FA_GeneraCodigoYRequiere2FA() {
        testUser.setTwoFactorEnabled(true);
        LoginRequest request = new LoginRequest("test@aerogest.com", "password123");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));

        AuthResponse response = authService.login(request);

        assertNotNull(response);
        assertTrue(response.getRequires2fa());
        assertNull(response.getToken());
        assertEquals("test@aerogest.com", response.getEmail());
        assertNotNull(testUser.getTwoFactorCode());
        assertEquals(6, testUser.getTwoFactorCode().length());
        assertNotNull(testUser.getTwoFactorCodeExpiry());
        assertTrue(testUser.getTwoFactorCodeExpiry().isAfter(LocalDateTime.now()));
        verify(emailService).sendTwoFactorCode(eq("test@aerogest.com"), anyString());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Login con credenciales incorrectas lanza BadCredentialsException")
    void login_CredencialesInvalidas_LanzaExcepcion() {
        LoginRequest request = new LoginRequest("test@aerogest.com", "wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciales inválidas"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("Verificación de email con token válido marca usuario como verificado")
    void verifyEmail_TokenValido_VerificaUsuario() {
        testUser.setIsVerified(false);
        testUser.setVerificationToken("valid-token-123");

        when(userRepository.findByVerificationToken("valid-token-123")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser, testUser.getId())).thenReturn("jwt-verified");

        AuthResponse response = authService.verifyEmail("valid-token-123");

        assertNotNull(response);
        assertEquals("jwt-verified", response.getToken());
        assertTrue(response.getIsVerified());
        assertTrue(testUser.getIsVerified());
        assertNull(testUser.getVerificationToken());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Verificación de email con token nulo o vacío es rechazada por seguridad")
    void verifyEmail_TokenVacio_LanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail(null));
        assertThrows(IllegalArgumentException.class, () -> authService.verifyEmail("   "));
        verify(userRepository, never()).findByVerificationToken(any());
    }

    @Test
    @DisplayName("Verificación de 2FA con código correcto y vigente emite JWT")
    void verify2fa_CodigoCorrecto_EmiteJWT() {
        testUser.setTwoFactorCode("123456");
        testUser.setTwoFactorCodeExpiry(LocalDateTime.now().plusMinutes(5));

        Verify2FARequest request = new Verify2FARequest();
        request.setEmail("test@aerogest.com");
        request.setCode("123456");

        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(testUser, testUser.getId())).thenReturn("jwt-2fa-success");

        AuthResponse response = authService.verify2fa(request);

        assertNotNull(response);
        assertEquals("jwt-2fa-success", response.getToken());
        assertNull(testUser.getTwoFactorCode());
        assertNull(testUser.getTwoFactorCodeExpiry());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Verificación de 2FA con código expirado es rechazada")
    void verify2fa_CodigoExpirado_LanzaExcepcion() {
        testUser.setTwoFactorCode("123456");
        testUser.setTwoFactorCodeExpiry(LocalDateTime.now().minusMinutes(1)); // Expirado

        Verify2FARequest request = new Verify2FARequest();
        request.setEmail("test@aerogest.com");
        request.setCode("123456");

        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> authService.verify2fa(request));
        assertTrue(ex.getMessage().contains("expirado"));
    }

    @Test
    @DisplayName("Reenviar verificación envía nuevo token a usuario no verificado")
    void resendVerificationEmail_UsuarioNoVerificado_EnviaNuevoToken() {
        testUser.setIsVerified(false);
        testUser.setVerificationToken("old-token");

        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));

        authService.resendVerificationEmail("test@aerogest.com");

        assertNotEquals("old-token", testUser.getVerificationToken());
        verify(emailService).sendVerificationEmail(eq("test@aerogest.com"), eq(testUser.getVerificationToken()));
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Reenviar verificación para cuenta ya verificada lanza excepción clara")
    void resendVerificationEmail_UsuarioYaVerificado_LanzaExcepcion() {
        testUser.setIsVerified(true);

        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.resendVerificationEmail("test@aerogest.com"));
        assertTrue(ex.getMessage().contains("ya se encuentra verificada"));
        verify(emailService, never()).sendVerificationEmail(anyString(), anyString());
    }

    @Test
    @DisplayName("Reset password con token válido y contraseña segura actualiza hash")
    void resetPassword_TokenValido_ActualizaPassword() {
        testUser.setResetToken("reset-token-abc");
        testUser.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token-abc");
        request.setNewPassword("NewSecurePassword123!");

        when(userRepository.findByResetToken("reset-token-abc")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode("NewSecurePassword123!")).thenReturn("newHashedPassword");

        authService.resetPassword(request);

        assertEquals("newHashedPassword", testUser.getPassword());
        assertNull(testUser.getResetToken());
        assertNull(testUser.getResetTokenExpiry());
        verify(userRepository).save(testUser);
    }

    @Test
    @DisplayName("Obtener perfil devuelve el rol real del usuario autenticado desde el servidor")
    void obtenerPerfil_UsuarioAutenticado_DevuelveRolConfiable() {
        testUser.setRole(Role.ROLE_ADMIN);
        testUser.setTwoFactorEnabled(true);

        var auth = new UsernamePasswordAuthenticationToken(
                testUser.getEmail(), null, testUser.getAuthorities());
        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("test@aerogest.com")).thenReturn(Optional.of(testUser));

        var perfil = authService.obtenerPerfil();

        assertNotNull(perfil);
        assertEquals("test@aerogest.com", perfil.getEmail());
        assertEquals("ROLE_ADMIN", perfil.getRole());
        assertTrue(perfil.getIsVerified());
        assertTrue(perfil.getTwoFactorEnabled());

        org.springframework.security.core.context.SecurityContextHolder.clearContext();
    }
}
