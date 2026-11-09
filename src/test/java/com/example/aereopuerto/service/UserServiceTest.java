package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.dto.ActualizarClienteAdminRequest;
import com.example.aereopuerto.auth.dto.CrearClienteAdminRequest;
import com.example.aereopuerto.auth.dto.UsuarioResponse;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.auth.service.AuthService;
import com.example.aereopuerto.auth.service.UserService;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.Sexo;
import com.example.aereopuerto.repository.AsignacionRepository;
import com.example.aereopuerto.repository.CarritoRepository;
import com.example.aereopuerto.repository.FavoritoRepository;
import com.example.aereopuerto.repository.PersonaRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private FavoritoRepository favoritoRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthService authService;

    @InjectMocks
    private UserService userService;

    private Persona personaBase;
    private User userBase;

    @BeforeEach
    void setUp() {
        personaBase = Persona.builder()
                .id(10)
                .nombre("Juan")
                .apellido("Pérez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("35111222")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .sexo(Sexo.MASCULINO)
                .build();

        userBase = User.builder()
                .id(1)
                .persona(personaBase)
                .email("juan.perez@example.com")
                .telefono("1122334455")
                .password("encoded_pass")
                .role(Role.ROLE_USUARIO)
                .perfilCompleto(true)
                .isVerified(true)
                .build();
    }

    @Test
    @DisplayName("crearCliente: da de alta persona y usuario exitosamente")
    void crearCliente_exitoso() {
        CrearClienteAdminRequest req = CrearClienteAdminRequest.builder()
                .nombre("Carlos")
                .apellido("Gómez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("40123456")
                .fechaNacimiento(LocalDate.of(1995, 3, 15))
                .sexo(Sexo.MASCULINO)
                .email("carlos@example.com")
                .telefono("1198765432")
                .password("secreto123")
                .role(Role.ROLE_USUARIO)
                .build();

        when(userRepository.existsByEmail("carlos@example.com")).thenReturn(false);
        when(personaRepository.findByNumeroIdentificador("40123456")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("secreto123")).thenReturn("hash_secreto123");
        when(personaRepository.save(any(Persona.class))).thenAnswer(i -> {
            Persona p = i.getArgument(0);
            p.setId(20);
            return p;
        });
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            u.setId(2);
            return u;
        });
        when(authService.mapearUsuarioResponse(any(User.class))).thenReturn(
                UsuarioResponse.builder()
                        .id(2)
                        .nombre("Carlos")
                        .apellido("Gómez")
                        .email("carlos@example.com")
                        .role(Role.ROLE_USUARIO)
                        .build()
        );

        UsuarioResponse resp = userService.crearCliente(req);

        assertNotNull(resp);
        assertEquals(2, resp.getId());
        assertEquals("carlos@example.com", resp.getEmail());
        verify(personaRepository).save(any(Persona.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("crearCliente: rechaza si el email ya existe")
    void crearCliente_emailDuplicado_lanzaExcepcion() {
        CrearClienteAdminRequest req = CrearClienteAdminRequest.builder()
                .nombre("Carlos")
                .apellido("Gómez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("40123456")
                .fechaNacimiento(LocalDate.of(1995, 3, 15))
                .sexo(Sexo.MASCULINO)
                .email("duplicado@example.com")
                .telefono("1198765432")
                .password("secreto123")
                .role(Role.ROLE_USUARIO)
                .build();

        when(userRepository.existsByEmail("duplicado@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.crearCliente(req));
        assertTrue(ex.getMessage().contains("email"));
        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("crearCliente: rechaza si el documento ya existe")
    void crearCliente_documentoDuplicado_lanzaExcepcion() {
        CrearClienteAdminRequest req = CrearClienteAdminRequest.builder()
                .nombre("Carlos")
                .apellido("Gómez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("40123456")
                .fechaNacimiento(LocalDate.of(1995, 3, 15))
                .sexo(Sexo.MASCULINO)
                .email("carlos@example.com")
                .telefono("1198765432")
                .password("secreto123")
                .role(Role.ROLE_USUARIO)
                .build();

        when(userRepository.existsByEmail("carlos@example.com")).thenReturn(false);
        when(personaRepository.findByNumeroIdentificador("40123456")).thenReturn(Optional.of(personaBase));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.crearCliente(req));
        assertTrue(ex.getMessage().contains("documento"));
        verify(personaRepository, never()).save(any());
    }

    @Test
    @DisplayName("actualizarCliente: modifica datos personales y de cuenta correctamente")
    void actualizarCliente_exitoso() {
        ActualizarClienteAdminRequest req = ActualizarClienteAdminRequest.builder()
                .nombre("Juan Carlos")
                .apellido("Pérez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("35111222")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .sexo(Sexo.MASCULINO)
                .email("juancarlos@example.com")
                .telefono("1199998888")
                .role(Role.ROLE_EMPLEADO)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(userBase));
        when(userRepository.existsByEmail("juancarlos@example.com")).thenReturn(false);
        when(personaRepository.findByNumeroIdentificador("35111222")).thenReturn(Optional.of(personaBase));
        when(personaRepository.save(any(Persona.class))).thenReturn(personaBase);
        when(userRepository.save(any(User.class))).thenReturn(userBase);
        when(authService.mapearUsuarioResponse(any(User.class))).thenReturn(
                UsuarioResponse.builder()
                        .id(1)
                        .nombre("Juan Carlos")
                        .email("juancarlos@example.com")
                        .role(Role.ROLE_EMPLEADO)
                        .build()
        );

        UsuarioResponse resp = userService.actualizarCliente(1, req);

        assertNotNull(resp);
        assertEquals("Juan Carlos", resp.getNombre());
        assertEquals(Role.ROLE_EMPLEADO, resp.getRole());
        verify(personaRepository).save(any(Persona.class));
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("actualizarCliente: rechaza si el documento pertenece a otra persona")
    void actualizarCliente_documentoConflicto_lanzaExcepcion() {
        Persona otraPersona = Persona.builder().id(99).numeroIdentificador("99999999").build();
        ActualizarClienteAdminRequest req = ActualizarClienteAdminRequest.builder()
                .nombre("Juan")
                .apellido("Pérez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("99999999")
                .fechaNacimiento(LocalDate.of(1990, 5, 20))
                .sexo(Sexo.MASCULINO)
                .email("juan.perez@example.com")
                .telefono("1122334455")
                .role(Role.ROLE_USUARIO)
                .build();

        when(userRepository.findById(1)).thenReturn(Optional.of(userBase));
        when(personaRepository.findByNumeroIdentificador("99999999")).thenReturn(Optional.of(otraPersona));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.actualizarCliente(1, req));
        assertTrue(ex.getMessage().contains("documento"));
    }

    @Test
    @DisplayName("eliminarCliente: elimina exitosamente si no tiene reservas ni asignaciones")
    void eliminarCliente_exitoso() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "pass")
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(userBase));
        when(reservaRepository.existsByPersonaId(10)).thenReturn(false);
        when(asignacionRepository.existsByEmpleadoId(10)).thenReturn(false);
        when(carritoRepository.findByPersonaId(10)).thenReturn(Optional.empty());
        when(favoritoRepository.findByPersonaId(10)).thenReturn(Collections.emptyList());

        userService.eliminarCliente(1);

        verify(userRepository).delete(userBase);
        verify(personaRepository).delete(personaBase);
    }

    @Test
    @DisplayName("eliminarCliente: rechaza si el usuario tiene reservas")
    void eliminarCliente_conReservas_lanzaExcepcion() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "pass")
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(userBase));
        when(reservaRepository.existsByPersonaId(10)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.eliminarCliente(1));
        assertTrue(ex.getMessage().contains("reservas"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminarCliente: rechaza si el empleado tiene asignaciones de vuelo")
    void eliminarCliente_conAsignaciones_lanzaExcepcion() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("admin@example.com", "pass")
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(userBase));
        when(reservaRepository.existsByPersonaId(10)).thenReturn(false);
        when(asignacionRepository.existsByEmpleadoId(10)).thenReturn(true);

        IllegalStateException ex = assertThrows(IllegalStateException.class,
                () -> userService.eliminarCliente(1));
        assertTrue(ex.getMessage().contains("asignaciones"));
        verify(userRepository, never()).delete(any());
    }

    @Test
    @DisplayName("eliminarCliente: rechaza si el administrador intenta autoeliminarse")
    void eliminarCliente_autoeliminacion_lanzaExcepcion() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("juan.perez@example.com", "pass")
        );

        when(userRepository.findById(1)).thenReturn(Optional.of(userBase));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.eliminarCliente(1));
        assertTrue(ex.getMessage().contains("propia cuenta"));
        verify(userRepository, never()).delete(any());
    }
}
