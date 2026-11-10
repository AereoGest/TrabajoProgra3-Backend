package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.dto.ImpactoCancelacionDTO;
import com.example.aereopuerto.model.Asiento;
import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.AsientoRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CancelacionVueloServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private AsientoRepository asientoRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private CancelacionVueloService cancelacionVueloService;

    private final Vuelo vuelo = Vuelo.builder().id(10).estado(estadoVuelo.CANCELADO).build();

    private Reserva reserva(int id, EstadoReserva estado, String valor, int personaId, Asiento... asientos) {
        Reserva reserva = Reserva.builder()
                .id(id)
                .codigoReserva("RES" + id)
                .estadoReserva(estado)
                .valor(new BigDecimal(valor))
                .persona(Persona.builder().id(personaId).nombre("Ana").build())
                .pasajes(new ArrayList<>())
                .build();
        for (Asiento asiento : asientos) {
            reserva.agregarPasaje(Pasaje.builder().asiento(asiento).build());
        }
        return reserva;
    }

    @Test
    @DisplayName("Cancela las reservas pagadas con reembolso del 100%, libera asientos y registra el motivo")
    void cancelar_reservaPagada_reembolsoTotal() {
        Asiento asiento = Asiento.builder().id(1).codigo("7A").ocupado(true).build();
        Reserva pagada = reserva(1, EstadoReserva.CONFIRMADO, "250000", 5, asiento);
        when(reservaRepository.findNoCanceladasPorVuelo(10)).thenReturn(List.of(pagada));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(User.builder().email("ana@mail.com").build()));

        ImpactoCancelacionDTO resultado = cancelacionVueloService.cancelarReservasDelVuelo(vuelo);

        assertEquals(EstadoReserva.CANCELADA, pagada.getEstadoReserva());
        assertEquals(new BigDecimal("250000"), pagada.getMontoReembolso());
        assertEquals(CancelacionVueloService.MOTIVO_VUELO_CANCELADO, pagada.getMotivoCancelacion());
        assertFalse(asiento.getOcupado());
        assertNull(pagada.getPasajes().get(0).getAsiento());
        assertEquals(1, resultado.reservas());
        assertEquals(1, resultado.pasajeros());
        verify(reservaRepository).saveAll(List.of(pagada));
    }

    @Test
    @DisplayName("Una reserva sin pagar (PROCESANDO) se cancela sin reembolso")
    void cancelar_reservaSinPagar_sinReembolso() {
        Reserva pendiente = reserva(2, EstadoReserva.PROCESANDO, "180000", 6);
        when(reservaRepository.findNoCanceladasPorVuelo(10)).thenReturn(List.of(pendiente));
        when(userRepository.findByPersonaId(6)).thenReturn(Optional.empty());

        ImpactoCancelacionDTO resultado = cancelacionVueloService.cancelarReservasDelVuelo(vuelo);

        assertEquals(EstadoReserva.CANCELADA, pendiente.getEstadoReserva());
        assertEquals(BigDecimal.ZERO, pendiente.getMontoReembolso());
        assertEquals(BigDecimal.ZERO, resultado.montoReembolso());
    }

    @Test
    @DisplayName("Publica el evento para mandar un mail a cada titular con cuenta")
    void cancelar_publicaEventoConAvisos() {
        Reserva conCuenta = reserva(1, EstadoReserva.CONFIRMADO, "100", 5);
        Reserva sinCuenta = reserva(2, EstadoReserva.ACTIVO, "200", 6);
        when(reservaRepository.findNoCanceladasPorVuelo(10)).thenReturn(List.of(conCuenta, sinCuenta));
        when(userRepository.findByPersonaId(5)).thenReturn(Optional.of(User.builder().email("ana@mail.com").build()));
        when(userRepository.findByPersonaId(6)).thenReturn(Optional.empty());

        cancelacionVueloService.cancelarReservasDelVuelo(vuelo);

        ArgumentCaptor<VueloCanceladoEvent> captor = ArgumentCaptor.forClass(VueloCanceladoEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        VueloCanceladoEvent evento = captor.getValue();
        assertSame(vuelo, evento.vuelo());
        assertEquals(1, evento.avisos().size());
        assertEquals("ana@mail.com", evento.avisos().get(0).email());
        assertEquals("RES1", evento.avisos().get(0).codigoReserva());
        assertEquals(new BigDecimal("100"), evento.avisos().get(0).montoReembolso());
    }

    @Test
    @DisplayName("Si el vuelo no tiene reservas activas no guarda nada ni manda mails")
    void cancelar_sinReservas_noHaceNada() {
        when(reservaRepository.findNoCanceladasPorVuelo(10)).thenReturn(List.of());

        ImpactoCancelacionDTO resultado = cancelacionVueloService.cancelarReservasDelVuelo(vuelo);

        assertEquals(0, resultado.reservas());
        verify(reservaRepository, never()).saveAll(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("El impacto suma reservas, pasajeros y reembolso sin modificar nada")
    void calcularImpacto_sumaSinModificar() {
        Asiento a1 = Asiento.builder().id(1).ocupado(true).build();
        Asiento a2 = Asiento.builder().id(2).ocupado(true).build();
        Reserva pagada = reserva(1, EstadoReserva.CONFIRMADO, "300", 5, a1, a2);
        Reserva pendiente = reserva(2, EstadoReserva.PROCESANDO, "150", 6, Asiento.builder().id(3).build());
        when(reservaRepository.findNoCanceladasPorVuelo(10)).thenReturn(List.of(pagada, pendiente));

        ImpactoCancelacionDTO impacto = cancelacionVueloService.calcularImpacto(10);

        assertEquals(2, impacto.reservas());
        assertEquals(3, impacto.pasajeros());
        assertEquals(new BigDecimal("300"), impacto.montoReembolso());
        assertEquals(EstadoReserva.CONFIRMADO, pagada.getEstadoReserva());
        assertTrue(a1.getOcupado());
        verify(reservaRepository, never()).saveAll(any());
    }
}
