package com.example.aereopuerto.service;

import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class PoliticaDevolucionServiceTest {

    private final PoliticaDevolucionService politica = new PoliticaDevolucionService();

    /** Reserva pagada hoy para un vuelo que sale dentro de {@code diasHastaElVuelo} días. */
    private Reserva reserva(int diasHastaElVuelo, EstadoReserva estado) {
        Vuelo vuelo = Vuelo.builder().id(1).fechaSalida(LocalDate.now().plusDays(diasHastaElVuelo)).build();
        Reserva reserva = Reserva.builder()
                .id(1)
                .estadoReserva(estado)
                .valor(new BigDecimal("100000"))
                .fechaReserva(LocalDateTime.now())
                .pasajes(new ArrayList<>())
                .build();
        reserva.agregarPasaje(Pasaje.builder().vuelo(vuelo).build());
        return reserva;
    }

    @ParameterizedTest(name = "faltan {0} días → reembolso {1}")
    @DisplayName("El reembolso depende de los días que faltan para la salida del vuelo")
    @CsvSource({
            "60, 100000",
            "30, 100000",
            "29, 50000.00",
            "20, 50000.00",
            "19, 15000.00",
            "11, 15000.00",
            "10, 0",
            "0, 0",
            "-3, 0"
    })
    void calcular_segunDiasHastaElVuelo(int dias, String esperado) {
        BigDecimal reembolso = politica.calcular(reserva(dias, EstadoReserva.CONFIRMADO));

        assertEquals(0, new BigDecimal(esperado).compareTo(reembolso),
                () -> "Esperado " + esperado + " y dio " + reembolso);
    }

    @Test
    @DisplayName("Una reserva hecha hoy para un vuelo lejano ya no da reembolso 0 (bug anterior)")
    void calcular_reservaDeHoyVueloLejano_devuelveTodo() {
        Reserva reserva = reserva(45, EstadoReserva.CONFIRMADO);

        assertEquals(new BigDecimal("100000"), politica.calcular(reserva));
    }

    @Test
    @DisplayName("Una reserva sin pagar no tiene nada que devolver")
    void calcular_reservaSinPagar_devuelveCero() {
        assertEquals(BigDecimal.ZERO, politica.calcular(reserva(60, EstadoReserva.PROCESANDO)));
    }

    @Test
    @DisplayName("Si la reserva no tiene pasajes no se puede saber la fecha del vuelo: no hay reembolso")
    void calcular_sinPasajes_devuelveCero() {
        Reserva reserva = Reserva.builder()
                .estadoReserva(EstadoReserva.CONFIRMADO)
                .valor(new BigDecimal("100000"))
                .pasajes(new ArrayList<>())
                .build();

        assertEquals(BigDecimal.ZERO, politica.calcular(reserva));
    }
}
