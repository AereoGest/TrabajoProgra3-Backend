package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AeropuertoInvalidoException;
import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.Exceptions.VueloInvalidoException;
import com.example.aereopuerto.dto.ValidacionVueloDTO;
import com.example.aereopuerto.dto.VueloDTO;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoAvion;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.AeropuertoRepository;
import com.example.aereopuerto.repository.AvionRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import com.example.aereopuerto.repository.VueloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VueloServiceTest {

    @Mock
    private VueloRepository vueloRepository;

    @Mock
    private AeropuertoRepository aeropuertoRepository;

    @Mock
    private AvionRepository avionRepository;

    @Mock
    private PasajeRepository pasajeRepository;

    @InjectMocks
    private VueloService vueloService;

    private Aeropuerto ezeiza;
    private Aeropuerto madrid;
    private Avion avionDisponible;
    private LocalDate maniana;

    @BeforeEach
    void setUp() {
        maniana = LocalDate.now().plusDays(1);
        ezeiza = Aeropuerto.builder().id(1).nombre("Ezeiza").codigoIata("EZE").ciudad("Buenos Aires").pais("Argentina").build();
        madrid = Aeropuerto.builder().id(2).nombre("Barajas").codigoIata("MAD").ciudad("Madrid").pais("España").build();
        avionDisponible = Avion.builder().id(7).identificador("LV-ABC").modelo("A320")
                .capacidadPasajeros(180).capacidadBodega(5000.0).estado(estadoAvion.DISPONIBLE).build();
    }

    private VueloDTO dtoValido() {
        VueloDTO dto = new VueloDTO();
        dto.setAeropuertoOrigenId(1);
        dto.setAeropuertoDestinoId(2);
        dto.setAvionId(7);
        dto.setFechaSalida(maniana);
        dto.setHoraSalida(LocalTime.of(10, 0));
        dto.setFechaLlegada(maniana);
        dto.setHoraLlegada(LocalTime.of(22, 0));
        dto.setPrecioVuelo(850000.0);
        dto.setEscala(false);
        return dto;
    }

    private void mockearReferencias() {
        when(aeropuertoRepository.findById(1)).thenReturn(Optional.of(ezeiza));
        when(aeropuertoRepository.findById(2)).thenReturn(Optional.of(madrid));
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionDisponible));
    }

    // ───────────────────────── Alta ─────────────────────────

    @Test
    @DisplayName("Alta válida: arma el vuelo desde los IDs y lo crea como PROGRAMADO por defecto")
    void crearVuelo_valido_creaProgramado() {
        mockearReferencias();
        when(vueloRepository.existeConflictoHorario(eq(7), any(), any(), any(), any(), eq(-1))).thenReturn(false);
        when(vueloRepository.save(any(Vuelo.class))).thenAnswer(inv -> inv.getArgument(0));

        Vuelo creado = vueloService.crearVuelo(dtoValido());

        assertEquals(estadoVuelo.PROGRAMADO, creado.getEstado());
        assertEquals(ezeiza, creado.getAeropuertoOrigen());
        assertEquals(madrid, creado.getAeropuertoDestino());
        assertEquals(avionDisponible, creado.getAvion());
        verify(vueloRepository).save(any(Vuelo.class));
    }

    @Test
    @DisplayName("Alta con campos obligatorios faltantes lanza excepción")
    void crearVuelo_sinAvion_lanzaExcepcion() {
        VueloDTO dto = dtoValido();
        dto.setAvionId(null);

        assertThrows(VueloInvalidoException.class, () -> vueloService.crearVuelo(dto));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con salida en el pasado lanza excepción")
    void crearVuelo_salidaEnElPasado_lanzaExcepcion() {
        VueloDTO dto = dtoValido();
        dto.setFechaSalida(LocalDate.now().minusDays(1));
        dto.setFechaLlegada(LocalDate.now().minusDays(1));

        assertThrows(VueloInvalidoException.class, () -> vueloService.crearVuelo(dto));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta en estado CANCELADO no está permitida")
    void crearVuelo_estadoCancelado_lanzaExcepcion() {
        VueloDTO dto = dtoValido();
        dto.setEstado(estadoVuelo.CANCELADO);

        assertThrows(VueloInvalidoException.class, () -> vueloService.crearVuelo(dto));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con origen igual a destino lanza excepción")
    void crearVuelo_origenIgualDestino_lanzaExcepcion() {
        VueloDTO dto = dtoValido();
        dto.setAeropuertoDestinoId(1);
        when(aeropuertoRepository.findById(1)).thenReturn(Optional.of(ezeiza));
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionDisponible));

        assertThrows(VueloInvalidoException.class, () -> vueloService.crearVuelo(dto));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con precio no positivo lanza excepción")
    void crearVuelo_precioInvalido_lanzaExcepcion() {
        VueloDTO dto = dtoValido();
        dto.setPrecioVuelo(0.0);
        mockearReferencias();

        assertThrows(VueloInvalidoException.class, () -> vueloService.crearVuelo(dto));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con aeropuerto inexistente lanza excepción")
    void crearVuelo_aeropuertoInexistente_lanzaExcepcion() {
        when(aeropuertoRepository.findById(1)).thenReturn(Optional.empty());

        assertThrows(AeropuertoInvalidoException.class, () -> vueloService.crearVuelo(dtoValido()));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con avión en mantenimiento lanza excepción")
    void crearVuelo_avionEnMantenimiento_lanzaExcepcion() {
        avionDisponible.setEstado(estadoAvion.MANTENIMIENTO);
        mockearReferencias();

        assertThrows(AvionInvalidoException.class, () -> vueloService.crearVuelo(dtoValido()));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con el avión ocupado en ese horario lanza excepción")
    void crearVuelo_conflictoHorario_lanzaExcepcion() {
        mockearReferencias();
        when(vueloRepository.existeConflictoHorario(eq(7), any(), any(), any(), any(), eq(-1))).thenReturn(true);

        assertThrows(AvionInvalidoException.class, () -> vueloService.crearVuelo(dtoValido()));
        verify(vueloRepository, never()).save(any());
    }

    // ───────────────────────── Modificación ─────────────────────────

    @Test
    @DisplayName("Modificación válida actualiza los datos del vuelo")
    void actualizarVuelo_valido_actualiza() {
        Vuelo existente = Vuelo.builder().id(3).estado(estadoVuelo.PROGRAMADO).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(existente));
        mockearReferencias();
        when(vueloRepository.existeConflictoHorario(eq(7), any(), any(), any(), any(), eq(3))).thenReturn(false);
        when(vueloRepository.save(any(Vuelo.class))).thenAnswer(inv -> inv.getArgument(0));

        VueloDTO dto = dtoValido();
        dto.setEstado(estadoVuelo.REPROGRAMADO);
        dto.setPrecioVuelo(990000.0);

        Vuelo actualizado = vueloService.actualizarVuelo(3, dto);

        assertEquals(estadoVuelo.REPROGRAMADO, actualizado.getEstado());
        assertEquals(990000.0, actualizado.getPrecioVuelo());
        assertEquals(madrid, actualizado.getAeropuertoDestino());
    }

    @Test
    @DisplayName("Modificación sin estado lanza excepción")
    void actualizarVuelo_sinEstado_lanzaExcepcion() {
        when(vueloRepository.findById(3)).thenReturn(Optional.of(Vuelo.builder().id(3).build()));

        assertThrows(VueloInvalidoException.class, () -> vueloService.actualizarVuelo(3, dtoValido()));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modificación de un vuelo inexistente lanza excepción")
    void actualizarVuelo_inexistente_lanzaExcepcion() {
        when(vueloRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(VueloInvalidoException.class, () -> vueloService.actualizarVuelo(99, dtoValido()));
    }

    @Test
    @DisplayName("Modificación a CANCELADO no valida disponibilidad del avión")
    void actualizarVuelo_aCancelado_noValidaAvion() {
        Vuelo existente = Vuelo.builder().id(3).estado(estadoVuelo.PROGRAMADO).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(existente));
        when(aeropuertoRepository.findById(1)).thenReturn(Optional.of(ezeiza));
        when(aeropuertoRepository.findById(2)).thenReturn(Optional.of(madrid));
        avionDisponible.setEstado(estadoAvion.MANTENIMIENTO);
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionDisponible));
        when(vueloRepository.save(any(Vuelo.class))).thenAnswer(inv -> inv.getArgument(0));

        VueloDTO dto = dtoValido();
        dto.setEstado(estadoVuelo.CANCELADO);

        Vuelo actualizado = vueloService.actualizarVuelo(3, dto);

        assertEquals(estadoVuelo.CANCELADO, actualizado.getEstado());
        verify(vueloRepository, never()).existeConflictoHorario(anyInt(), any(), any(), any(), any(), anyInt());
    }

    // ───────────────────────── Baja ─────────────────────────

    @Test
    @DisplayName("Baja lógica: el vuelo pasa a CANCELADO y no se borra")
    void darDeBajaVuelo_programado_quedaCancelado() {
        Vuelo vuelo = Vuelo.builder().id(3).estado(estadoVuelo.PROGRAMADO).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(vuelo));
        when(vueloRepository.save(any(Vuelo.class))).thenAnswer(inv -> inv.getArgument(0));

        Vuelo resultado = vueloService.darDeBajaVuelo(3);

        assertEquals(estadoVuelo.CANCELADO, resultado.getEstado());
        verify(vueloRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Baja de un vuelo ya cancelado lanza excepción")
    void darDeBajaVuelo_yaCancelado_lanzaExcepcion() {
        Vuelo vuelo = Vuelo.builder().id(3).estado(estadoVuelo.CANCELADO).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(vuelo));

        assertThrows(VueloInvalidoException.class, () -> vueloService.darDeBajaVuelo(3));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Baja de un vuelo que ya finalizó lanza excepción")
    void darDeBajaVuelo_finalizado_lanzaExcepcion() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        Vuelo vuelo = Vuelo.builder().id(3).estado(estadoVuelo.PROGRAMADO)
                .fechaSalida(ayer).horaSalida(LocalTime.of(8, 0))
                .fechaLlegada(ayer).horaLlegada(LocalTime.of(10, 0)).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(vuelo));

        assertThrows(VueloInvalidoException.class, () -> vueloService.darDeBajaVuelo(3));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modificación de un vuelo que ya finalizó lanza excepción")
    void actualizarVuelo_finalizado_lanzaExcepcion() {
        LocalDate ayer = LocalDate.now().minusDays(1);
        Vuelo vuelo = Vuelo.builder().id(3).estado(estadoVuelo.PROGRAMADO)
                .fechaSalida(ayer).horaSalida(LocalTime.of(8, 0))
                .fechaLlegada(ayer).horaLlegada(LocalTime.of(10, 0)).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(vuelo));

        VueloDTO dto = dtoValido();
        dto.setEstado(estadoVuelo.PROGRAMADO);

        assertThrows(VueloInvalidoException.class, () -> vueloService.actualizarVuelo(3, dto));
        verify(vueloRepository, never()).save(any());
    }

    // ───────────────────────── Misma ciudad ─────────────────────────

    @Test
    @DisplayName("Alta entre dos aeropuertos de la misma ciudad (EZE → AEP) lanza excepción")
    void crearVuelo_mismaCiudad_lanzaExcepcion() {
        Aeropuerto aeroparque = Aeropuerto.builder().id(3).nombre("Aeroparque").codigoIata("AEP")
                .ciudad("buenos  aires").pais("Argentina").build();
        VueloDTO dto = dtoValido();
        dto.setAeropuertoDestinoId(3);
        when(aeropuertoRepository.findById(1)).thenReturn(Optional.of(ezeiza));
        when(aeropuertoRepository.findById(3)).thenReturn(Optional.of(aeroparque));
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionDisponible));

        VueloInvalidoException ex = assertThrows(VueloInvalidoException.class, () -> vueloService.crearVuelo(dto));
        assertTrue(ex.getMessage().contains("misma ciudad"));
        verify(vueloRepository, never()).save(any());
    }

    // ───────────────────────── Validación en vivo ─────────────────────────

    @Test
    @DisplayName("Validar alta: devuelve el error cuando el avión ya tiene un vuelo en ese horario, sin guardar")
    void validarVuelo_conflictoHorario_devuelveError() {
        mockearReferencias();
        when(vueloRepository.existeConflictoHorario(eq(7), any(), any(), any(), any(), eq(-1))).thenReturn(true);

        ValidacionVueloDTO resultado = vueloService.validarVuelo(null, dtoValido());

        assertFalse(resultado.valido());
        assertTrue(resultado.mensaje().contains("ya tiene un vuelo"));
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validar alta con datos correctos devuelve válido, sin guardar")
    void validarVuelo_valido_devuelveOk() {
        mockearReferencias();
        when(vueloRepository.existeConflictoHorario(eq(7), any(), any(), any(), any(), eq(-1))).thenReturn(false);

        ValidacionVueloDTO resultado = vueloService.validarVuelo(null, dtoValido());

        assertTrue(resultado.valido());
        assertNull(resultado.mensaje());
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Validar modificación excluye al propio vuelo del chequeo de horario y no toca la entidad")
    void validarVuelo_modificacion_excluyePropioVuelo() {
        Vuelo existente = Vuelo.builder().id(3).estado(estadoVuelo.PROGRAMADO).precioVuelo(100.0).build();
        when(vueloRepository.findById(3)).thenReturn(Optional.of(existente));
        mockearReferencias();
        when(vueloRepository.existeConflictoHorario(eq(7), any(), any(), any(), any(), eq(3))).thenReturn(false);

        VueloDTO dto = dtoValido();
        dto.setEstado(estadoVuelo.PROGRAMADO);

        ValidacionVueloDTO resultado = vueloService.validarVuelo(3, dto);

        assertTrue(resultado.valido());
        assertEquals(100.0, existente.getPrecioVuelo());
        verify(vueloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Baja de un vuelo inexistente lanza excepción")
    void darDeBajaVuelo_inexistente_lanzaExcepcion() {
        when(vueloRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(VueloInvalidoException.class, () -> vueloService.darDeBajaVuelo(99));
    }
}
