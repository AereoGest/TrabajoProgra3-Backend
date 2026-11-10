package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.dto.AvionDTO;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoAvion;
import com.example.aereopuerto.model.enums.estadoVuelo;
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
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvionServiceTest {

    @Mock
    private AvionRepository avionRepository;

    @Mock
    private VueloRepository vueloRepository;

    @Mock
    private PasajeRepository pasajeRepository;

    @Mock
    private AsientoService asientoService;

    @InjectMocks
    private AvionService avionService;

    private Avion avionExistente;

    @BeforeEach
    void setUp() {
        avionExistente = Avion.builder()
                .id(7)
                .identificador("LV-A100")
                .modelo("Airbus A320")
                .capacidadPasajeros(180)
                .capacidadBodega(5000.0)
                .estado(estadoAvion.DISPONIBLE)
                .build();
    }

    private AvionDTO dtoValido() {
        AvionDTO dto = new AvionDTO();
        dto.setIdentificador("lv-a200 ");
        dto.setModelo("  Boeing   737-800 ");
        dto.setCapacidadPasajeros(189);
        dto.setCapacidadBodega(4500.0);
        return dto;
    }

    private Vuelo vueloFuturo(int id) {
        LocalDate maniana = LocalDate.now().plusDays(1);
        return Vuelo.builder().id(id).estado(estadoVuelo.PROGRAMADO)
                .fechaSalida(maniana).horaSalida(LocalTime.of(10, 0))
                .fechaLlegada(maniana).horaLlegada(LocalTime.of(14, 0))
                .build();
    }

    private Vuelo vueloPasadoSinActualizar(int id) {
        LocalDate ayer = LocalDate.now().minusDays(1);
        return Vuelo.builder().id(id).estado(estadoVuelo.PROGRAMADO)
                .fechaSalida(ayer).horaSalida(LocalTime.of(10, 0))
                .fechaLlegada(ayer).horaLlegada(LocalTime.of(14, 0))
                .build();
    }

    // ───────────────────────── Alta ─────────────────────────

    @Test
    @DisplayName("Alta válida: normaliza identificador y modelo, y crea el avión como DISPONIBLE")
    void crearAvion_valido_creaDisponible() {
        when(avionRepository.existsByIdentificadorIgnoreCase("LV-A200")).thenReturn(false);
        when(avionRepository.save(any(Avion.class))).thenAnswer(inv -> inv.getArgument(0));

        Avion creado = avionService.crearAvion(dtoValido());

        assertEquals("LV-A200", creado.getIdentificador());
        assertEquals("Boeing 737-800", creado.getModelo());
        assertEquals(estadoAvion.DISPONIBLE, creado.getEstado());
        verify(avionRepository).save(any(Avion.class));
        verify(asientoService).sincronizarAsientos(creado);
    }

    @Test
    @DisplayName("Alta con identificador repetido lanza excepción")
    void crearAvion_identificadorRepetido_lanzaExcepcion() {
        when(avionRepository.existsByIdentificadorIgnoreCase("LV-A200")).thenReturn(true);

        AvionInvalidoException ex = assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(dtoValido()));
        assertTrue(ex.getMessage().contains("Ya existe"));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con identificador en formato inválido lanza excepción")
    void crearAvion_formatoInvalido_lanzaExcepcion() {
        AvionDTO dto = dtoValido();
        dto.setIdentificador("ABC-123");

        assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(dto));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Acepta matrículas argentinas (LV-KFX) y el formato interno (LV-A25)")
    void crearAvion_formatosValidos_seCrean() {
        when(avionRepository.existsByIdentificadorIgnoreCase(any())).thenReturn(false);
        when(avionRepository.save(any(Avion.class))).thenAnswer(inv -> inv.getArgument(0));

        for (String matricula : List.of("lv-kfx", "LV-A25", "LV-A001")) {
            AvionDTO dto = dtoValido();
            dto.setIdentificador(matricula);
            assertDoesNotThrow(() -> avionService.crearAvion(dto), matricula);
        }
    }

    @Test
    @DisplayName("Rechaza matrículas que no cumplen ninguno de los dos formatos")
    void crearAvion_formatosInvalidos_lanzaExcepcion() {
        for (String matricula : List.of("LV-KF", "LV-KFXX", "LV-B12", "LV-12A", "LVKFX")) {
            AvionDTO dto = dtoValido();
            dto.setIdentificador(matricula);
            assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(dto), matricula);
        }
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta sin modelo lanza excepción")
    void crearAvion_sinModelo_lanzaExcepcion() {
        AvionDTO dto = dtoValido();
        dto.setModelo("   ");

        assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(dto));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta con capacidades fuera de rango lanza excepción")
    void crearAvion_capacidadesInvalidas_lanzaExcepcion() {
        AvionDTO sinPasajeros = dtoValido();
        sinPasajeros.setCapacidadPasajeros(0);
        AvionDTO demasiados = dtoValido();
        demasiados.setCapacidadPasajeros(AvionService.MAX_CAPACIDAD_PASAJEROS + 1);
        AvionDTO sinBodega = dtoValido();
        sinBodega.setCapacidadBodega(-1.0);

        assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(sinPasajeros));
        assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(demasiados));
        assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(sinBodega));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Alta en estado BAJA no está permitida")
    void crearAvion_enBaja_lanzaExcepcion() {
        AvionDTO dto = dtoValido();
        dto.setEstado(estadoAvion.BAJA);

        assertThrows(AvionInvalidoException.class, () -> avionService.crearAvion(dto));
        verify(avionRepository, never()).save(any());
    }

    // ───────────────────────── Modificación ─────────────────────────

    @Test
    @DisplayName("Modificación válida actualiza los datos del avión")
    void actualizarAvion_valido_actualiza() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(avionRepository.existsByIdentificadorIgnoreCaseAndIdNot("LV-A200", 7)).thenReturn(false);
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of());
        when(avionRepository.save(any(Avion.class))).thenAnswer(inv -> inv.getArgument(0));

        AvionDTO dto = dtoValido();
        dto.setEstado(estadoAvion.ACTIVO);

        Avion actualizado = avionService.actualizarAvion(7, dto);

        assertEquals("LV-A200", actualizado.getIdentificador());
        assertEquals(189, actualizado.getCapacidadPasajeros());
        assertEquals(estadoAvion.ACTIVO, actualizado.getEstado());
        verify(asientoService).validarCambioCapacidad(7, 189);
        verify(asientoService).sincronizarAsientos(actualizado);
    }

    @Test
    @DisplayName("Si los asientos no permiten achicar la capacidad, no se guarda el avión")
    void actualizarAvion_asientosOcupados_noGuarda() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(avionRepository.existsByIdentificadorIgnoreCaseAndIdNot("LV-A200", 7)).thenReturn(false);
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of());
        doThrow(new AvionInvalidoException("sin asientos libres"))
                .when(asientoService).validarCambioCapacidad(7, 100);

        AvionDTO dto = dtoValido();
        dto.setEstado(estadoAvion.DISPONIBLE);
        dto.setCapacidadPasajeros(100);

        assertThrows(AvionInvalidoException.class, () -> avionService.actualizarAvion(7, dto));
        assertEquals(180, avionExistente.getCapacidadPasajeros());
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modificación inválida no altera la entidad")
    void actualizarAvion_invalido_noModificaEntidad() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));

        AvionDTO dto = dtoValido();
        dto.setEstado(estadoAvion.DISPONIBLE);
        dto.setIdentificador("MAL");

        assertThrows(AvionInvalidoException.class, () -> avionService.actualizarAvion(7, dto));
        assertEquals("LV-A100", avionExistente.getIdentificador());
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede pasar a MANTENIMIENTO un avión con vuelos pendientes")
    void actualizarAvion_aMantenimientoConVuelos_lanzaExcepcion() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(avionRepository.existsByIdentificadorIgnoreCaseAndIdNot("LV-A200", 7)).thenReturn(false);
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of(vueloFuturo(30)));

        AvionDTO dto = dtoValido();
        dto.setEstado(estadoAvion.MANTENIMIENTO);

        AvionInvalidoException ex = assertThrows(AvionInvalidoException.class, () -> avionService.actualizarAvion(7, dto));
        assertTrue(ex.getMessage().contains("#30"));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("No se puede reducir la capacidad por debajo de los pasajes vendidos")
    void actualizarAvion_reducirCapacidadBajoVendidos_lanzaExcepcion() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(avionRepository.existsByIdentificadorIgnoreCaseAndIdNot("LV-A200", 7)).thenReturn(false);
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of(vueloFuturo(30)));
        when(pasajeRepository.countByVueloId(30)).thenReturn(150L);

        AvionDTO dto = dtoValido();
        dto.setEstado(estadoAvion.DISPONIBLE);
        dto.setCapacidadPasajeros(120);

        assertThrows(AvionInvalidoException.class, () -> avionService.actualizarAvion(7, dto));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Modificación de un avión inexistente devuelve 404")
    void actualizarAvion_inexistente_404() {
        when(avionRepository.findById(99)).thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> avionService.actualizarAvion(99, dtoValido()));
    }

    // ───────────────────────── Baja ─────────────────────────

    @Test
    @DisplayName("Baja lógica: el avión pasa a BAJA y no se borra")
    void darDeBajaAvion_sinVuelos_quedaEnBaja() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of());
        when(avionRepository.save(any(Avion.class))).thenAnswer(inv -> inv.getArgument(0));

        Avion resultado = avionService.darDeBajaAvion(7);

        assertEquals(estadoAvion.BAJA, resultado.getEstado());
        verify(avionRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Baja de un avión con vuelos pendientes lanza excepción")
    void darDeBajaAvion_conVuelosPendientes_lanzaExcepcion() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of(vueloFuturo(30)));

        assertThrows(AvionInvalidoException.class, () -> avionService.darDeBajaAvion(7));
        verify(avionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Los vuelos que ya finalizaron no impiden la baja aunque sigan como PROGRAMADO")
    void darDeBajaAvion_vuelosFinalizados_noBloquean() {
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));
        when(vueloRepository.findByAvionIdAndEstadoIn(eq(7), anyList())).thenReturn(List.of(vueloPasadoSinActualizar(12)));
        when(avionRepository.save(any(Avion.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(estadoAvion.BAJA, avionService.darDeBajaAvion(7).getEstado());
    }

    @Test
    @DisplayName("Baja de un avión que ya está de baja lanza excepción")
    void darDeBajaAvion_yaEnBaja_lanzaExcepcion() {
        avionExistente.setEstado(estadoAvion.BAJA);
        when(avionRepository.findById(7)).thenReturn(Optional.of(avionExistente));

        assertThrows(AvionInvalidoException.class, () -> avionService.darDeBajaAvion(7));
        verify(avionRepository, never()).save(any());
    }
}
