package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.ReservaInvalidaException;
import com.example.aereopuerto.dto.PasajeDTO;
import com.example.aereopuerto.model.*;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasajeServiceTest {

    @Mock
    private PasajeRepository pasajeRepository;
    @Mock
    private VueloRepository vueloRepository;
    @Mock
    private EquipajeRepository equipajeRepository;
    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private AsientoRepository asientoRepository;
    @Mock
    private AsistenciaRepository asistenciaRepository;

    @InjectMocks
    private PasajeService pasajeService;

    private Avion avion;
    private Vuelo vuelo;
    private Reserva reserva;
    private Asiento asientoOriginal;
    private Pasaje pasaje1;
    private Pasaje pasaje2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(pasajeService, "tasaImpuesto", new BigDecimal("0.21"));
        ReflectionTestUtils.setField(pasajeService, "tasaServicio", new BigDecimal("0.05"));

        avion = Avion.builder()
                .id(1)
                .modelo("Boeing 737")
                .capacidadPasajeros(100)
                .build();

        vuelo = Vuelo.builder()
                .id(5)
                .precioVuelo(100000.0)
                .estado(estadoVuelo.PROGRAMADO)
                .avion(avion)
                .build();

        asientoOriginal = Asiento.builder()
                .id(10)
                .codigo("1A")
                .ocupado(true)
                .precioExtra(0.0)
                .avion(avion)
                .build();

        reserva = Reserva.builder()
                .id(50)
                .codigoReserva("AC-2026-PAS1")
                .estadoReserva(EstadoReserva.CONFIRMADO)
                .valor(new BigDecimal("254100"))
                .cantidadPasajes(2)
                .pasajes(new ArrayList<>())
                .build();

        pasaje1 = Pasaje.builder()
                .id(100)
                .codigoPasaje("COD001")
                .clasesVuelo(ClasesVuelo.ECONOMICA)
                .vuelo(vuelo)
                .asiento(asientoOriginal)
                .equipajes(new ArrayList<>())
                .reserva(reserva)
                .nombre("Juan")
                .apellido("Pérez")
                .tipoDocumento(Identificador.DNI)
                .documento("35111222")
                .fechaNacimiento(LocalDate.of(1990, 5, 10))
                .email("juan@aerogest.com")
                .build();

        pasaje2 = Pasaje.builder()
                .id(101)
                .codigoPasaje("COD002")
                .clasesVuelo(ClasesVuelo.ECONOMICA)
                .vuelo(vuelo)
                .equipajes(new ArrayList<>())
                .reserva(reserva)
                .nombre("Ana")
                .apellido("Pérez")
                .tipoDocumento(Identificador.DNI)
                .documento("38333444")
                .fechaNacimiento(LocalDate.of(1993, 8, 20))
                .email("ana@aerogest.com")
                .build();

        reserva.getPasajes().add(pasaje1);
        reserva.getPasajes().add(pasaje2);
    }

    @Test
    @DisplayName("editarPasaje actualiza datos del pasajero, libera asiento anterior, ocupa el nuevo y recalcula el valor de la reserva")
    void editarPasaje_CambioDeAsientoYExtras_LiberaAsientoAnteriorYRecalculaTotal() {
        Asiento nuevoAsiento = Asiento.builder()
                .id(11)
                .codigo("2B")
                .ocupado(false)
                .precioExtra(10000.0)
                .avion(avion)
                .build();

        Equipaje equipajeBodega = Equipaje.builder()
                .id(3)
                .precio(20000.0)
                .build();

        AsistenciaAlViajero asistencia = AsistenciaAlViajero.builder()
                .id(2)
                .nombrePlan("Premium")
                .precio(15000.0)
                .build();

        PasajeDTO dto = new PasajeDTO();
        dto.setReservaId(50);
        dto.setVueloId(5);
        dto.setClasesVuelo(ClasesVuelo.BUSINESS);
        dto.setAsientoId(11);
        dto.setEquipajeIds(List.of(3));
        dto.setAsistenciaId(2);
        dto.setNombre("Juan Carlos");
        dto.setApellido("Pérez");
        dto.setTipoDocumento(Identificador.DNI);
        dto.setDocumento("35111222");
        dto.setFechaNacimiento(LocalDate.of(1990, 5, 10));
        dto.setEmail("jc@aerogest.com");

        when(pasajeRepository.findById(100)).thenReturn(Optional.of(pasaje1));
        when(asientoRepository.findById(11)).thenReturn(Optional.of(nuevoAsiento));
        when(equipajeRepository.findById(3)).thenReturn(Optional.of(equipajeBodega));
        when(asistenciaRepository.findById(2)).thenReturn(Optional.of(asistencia));
        when(pasajeRepository.save(any(Pasaje.class))).thenAnswer(inv -> inv.getArgument(0));

        Pasaje actualizado = pasajeService.editarPasaje(100, dto);

        assertThat(actualizado.getNombre()).isEqualTo("Juan Carlos");
        assertThat(actualizado.getClasesVuelo()).isEqualTo(ClasesVuelo.BUSINESS);
        assertThat(actualizado.getAsiento()).isEqualTo(nuevoAsiento);
        assertThat(asientoOriginal.getOcupado()).isFalse();
        assertThat(nuevoAsiento.getOcupado()).isTrue();
        // Base 2 pasajes = 200000; impuestos (21%) = 42000; extras = 10000 + 20000 + 15000 = 45000
        // Subtotal = 287000; servicio (5%) = 14350 -> Total = 301350
        assertThat(reserva.getValor()).isEqualByComparingTo("301350");
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("editarPasaje rechaza modificar un pasaje si no pertenece a la reserva indicada")
    void editarPasaje_ReservaIncorrecta_LanzaReservaInvalidaException() {
        PasajeDTO dto = new PasajeDTO();
        dto.setReservaId(999);
        dto.setClasesVuelo(ClasesVuelo.ECONOMICA);
        dto.setNombre("Juan");
        dto.setApellido("Pérez");
        dto.setDocumento("35111222");

        when(pasajeRepository.findById(100)).thenReturn(Optional.of(pasaje1));

        assertThatThrownBy(() -> pasajeService.editarPasaje(100, dto))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessageContaining("no pertenece a la reserva indicada");
    }

    @Test
    @DisplayName("editarPasaje rechaza documento duplicado con otro pasajero de la misma reserva")
    void editarPasaje_DocumentoDuplicadoEnMismaReserva_LanzaReservaInvalidaException() {
        PasajeDTO dto = new PasajeDTO();
        dto.setReservaId(50);
        dto.setClasesVuelo(ClasesVuelo.ECONOMICA);
        dto.setNombre("Juan");
        dto.setApellido("Pérez");
        dto.setTipoDocumento(Identificador.DNI);
        dto.setDocumento("38333444"); // Documento de pasaje2

        when(pasajeRepository.findById(100)).thenReturn(Optional.of(pasaje1));

        assertThatThrownBy(() -> pasajeService.editarPasaje(100, dto))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessageContaining("ya pertenece a otro pasajero en la misma reserva");
    }

    @Test
    @DisplayName("cancelarPasajeDeReserva elimina un pasaje, libera su asiento y recalcula cantidadPasajes y total de la reserva")
    void cancelarPasajeDeReserva_QuedanOtrosPasajes_LiberaAsientoYRecalculaReserva() {
        when(pasajeRepository.findById(100)).thenReturn(Optional.of(pasaje1));

        pasajeService.cancelarPasajeDeReserva(100, 50);

        assertThat(asientoOriginal.getOcupado()).isFalse();
        assertThat(reserva.getPasajes()).hasSize(1);
        assertThat(reserva.getCantidadPasajes()).isEqualTo(1);
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADO);
        // 1 pasaje base 100000 + 21000 imp = 121000 + 5% (6050) = 127050
        assertThat(reserva.getValor()).isEqualByComparingTo("127050");
        verify(pasajeRepository).delete(pasaje1);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("cancelarPasajeDeReserva pasa la reserva a CANCELADA cuando se cancela su último pasaje")
    void cancelarPasajeDeReserva_UltimoPasaje_PasaReservaACancelada() {
        reserva.getPasajes().remove(pasaje2);
        reserva.setCantidadPasajes(1);

        when(pasajeRepository.findById(100)).thenReturn(Optional.of(pasaje1));

        pasajeService.cancelarPasajeDeReserva(100, 50);

        assertThat(asientoOriginal.getOcupado()).isFalse();
        assertThat(reserva.getPasajes()).isEmpty();
        assertThat(reserva.getCantidadPasajes()).isZero();
        assertThat(reserva.getValor()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        verify(pasajeRepository).delete(pasaje1);
        verify(reservaRepository).save(reserva);
    }
}
