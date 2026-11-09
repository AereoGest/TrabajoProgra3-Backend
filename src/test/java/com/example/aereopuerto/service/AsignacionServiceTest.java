package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.dto.AsignacionDTO;
import com.example.aereopuerto.model.Asignacion;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.RolEmpleado;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.AsignacionRepository;
import com.example.aereopuerto.repository.PersonaRepository;
import com.example.aereopuerto.repository.VueloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsignacionServiceTest {

    @Mock
    private AsignacionRepository asignacionRepository;

    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;

    @Mock
    private VueloRepository vueloRepository;

    @Mock
    private PersonaRepository personaRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AsignacionService asignacionService;

    private Vuelo vueloValido;
    private Persona personaEmpleado;
    private User userEmpleado;

    @BeforeEach
    void setUp() {
        LocalDate maniana = LocalDate.now().plusDays(1);
        vueloValido = Vuelo.builder()
                .id(10)
                .estado(estadoVuelo.PROGRAMADO)
                .fechaSalida(maniana)
                .horaSalida(LocalTime.of(10, 0))
                .fechaLlegada(maniana)
                .horaLlegada(LocalTime.of(12, 0))
                .build();

        personaEmpleado = Persona.builder()
                .id(5)
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        userEmpleado = User.builder()
                .id(20)
                .persona(personaEmpleado)
                .role(Role.ROLE_EMPLEADO)
                .email("juan.perez@aerogest.com")
                .build();
    }

    @Test
    @DisplayName("crearAsignacion: crea asignación exitosamente")
    void crearAsignacion_Exitoso() {
        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);

        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));
        when(personaRepository.findById(5)).thenReturn(Optional.of(personaEmpleado));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(userEmpleado));
        when(asignacionRepository.existsByVueloIdAndEmpleadoId(10, 5)).thenReturn(false);
        when(asignacionRepository.findByEmpleadoId(5)).thenReturn(List.of());
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(invocation -> {
            Asignacion a = invocation.getArgument(0);
            a.setId(1);
            return a;
        });

        Asignacion res = asignacionService.crearAsignacion(dto);

        assertNotNull(res);
        assertEquals(1, res.getId());
        assertEquals(RolEmpleado.PILOTO, res.getRolEmpleado());
        assertEquals(10, res.getVuelo().getId());
        assertEquals(5, res.getEmpleado().getId());
        verify(asignacionRepository).save(any(Asignacion.class));
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si el vuelo no existe")
    void crearAsignacion_VueloNoExiste() {
        AsignacionDTO dto = new AsignacionDTO(999, 5, RolEmpleado.PILOTO);
        when(vueloRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si el vuelo está cancelado")
    void crearAsignacion_VueloCancelado() {
        vueloValido.setEstado(estadoVuelo.CANCELADO);
        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);
        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("cancelado"));
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si el vuelo ya finalizó")
    void crearAsignacion_VueloFinalizado() {
        vueloValido.setFechaLlegada(LocalDate.now().minusDays(1));
        vueloValido.setFechaSalida(LocalDate.now().minusDays(1));
        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);
        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("finalizado"));
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si el empleado no existe")
    void crearAsignacion_EmpleadoNoExiste() {
        AsignacionDTO dto = new AsignacionDTO(10, 999, RolEmpleado.PILOTO);
        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));
        when(personaRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si la persona no es empleado ni admin")
    void crearAsignacion_PersonaNoEsEmpleado() {
        userEmpleado.setRole(Role.ROLE_USUARIO);
        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);

        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));
        when(personaRepository.findById(5)).thenReturn(Optional.of(personaEmpleado));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(userEmpleado));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertTrue(ex.getReason().contains("no posee rol de empleado"));
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si el empleado ya está asignado al vuelo (duplicado)")
    void crearAsignacion_Duplicado() {
        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);

        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));
        when(personaRepository.findById(5)).thenReturn(Optional.of(personaEmpleado));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(userEmpleado));
        when(asignacionRepository.existsByVueloIdAndEmpleadoId(10, 5)).thenReturn(true);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("ya se encuentra asignado"));
    }

    @Test
    @DisplayName("crearAsignacion: rechaza si el empleado tiene superposición horaria con otro vuelo")
    void crearAsignacion_ConflictoHorario() {
        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);

        LocalDate maniana = vueloValido.getFechaSalida();
        Vuelo vueloSolapado = Vuelo.builder()
                .id(99)
                .estado(estadoVuelo.PROGRAMADO)
                .fechaSalida(maniana)
                .horaSalida(LocalTime.of(11, 0)) // dentro del rango 10:00 - 12:00
                .fechaLlegada(maniana)
                .horaLlegada(LocalTime.of(13, 0))
                .build();

        Asignacion asignacionPrevia = Asignacion.builder()
                .id(2)
                .vuelo(vueloSolapado)
                .empleado(personaEmpleado)
                .rolEmpleado(RolEmpleado.COPILOTO)
                .build();

        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));
        when(personaRepository.findById(5)).thenReturn(Optional.of(personaEmpleado));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(userEmpleado));
        when(asignacionRepository.existsByVueloIdAndEmpleadoId(10, 5)).thenReturn(false);
        when(asignacionRepository.findByEmpleadoId(5)).thenReturn(List.of(asignacionPrevia));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.crearAsignacion(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertTrue(ex.getReason().contains("conflicto horario"));
    }

    @Test
    @DisplayName("actualizarAsignacion: actualiza rol y datos correctamente")
    void actualizarAsignacion_Exitoso() {
        Asignacion existente = Asignacion.builder()
                .id(1)
                .vuelo(vueloValido)
                .empleado(personaEmpleado)
                .rolEmpleado(RolEmpleado.COPILOTO)
                .build();

        AsignacionDTO dto = new AsignacionDTO(10, 5, RolEmpleado.PILOTO);

        when(asignacionRepository.findById(1)).thenReturn(Optional.of(existente));
        when(vueloRepository.findById(10)).thenReturn(Optional.of(vueloValido));
        when(personaRepository.findById(5)).thenReturn(Optional.of(personaEmpleado));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(userEmpleado));
        when(asignacionRepository.existsByVueloIdAndEmpleadoIdAndIdNot(10, 5, 1)).thenReturn(false);
        when(asignacionRepository.findByEmpleadoId(5)).thenReturn(List.of(existente));
        when(asignacionRepository.save(any(Asignacion.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Asignacion res = asignacionService.actualizarAsignacion(1, dto);

        assertNotNull(res);
        assertEquals(RolEmpleado.PILOTO, res.getRolEmpleado());
    }

    @Test
    @DisplayName("eliminarAsignacion: elimina asignación existente")
    void eliminarAsignacion_Exitoso() {
        Asignacion existente = Asignacion.builder()
                .id(1)
                .vuelo(vueloValido)
                .empleado(personaEmpleado)
                .rolEmpleado(RolEmpleado.AZAFATA)
                .build();

        when(asignacionRepository.findById(1)).thenReturn(Optional.of(existente));

        asignacionService.eliminarAsignacion(1);

        verify(asignacionRepository).delete(existente);
    }

    @Test
    @DisplayName("eliminarAsignacion: lanza 404 si la asignación no existe")
    void eliminarAsignacion_NoExiste() {
        when(asignacionRepository.findById(999)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> asignacionService.eliminarAsignacion(999));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
