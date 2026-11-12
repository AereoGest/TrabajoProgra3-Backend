package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.service.EmailService;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.model.Vuelo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificadorCancelacionVueloTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificadorCancelacionVuelo notificador;

    private final Vuelo vuelo = Vuelo.builder()
            .id(10)
            .aeropuertoOrigen(Aeropuerto.builder().ciudad("Buenos Aires").codigoIata("EZE").build())
            .aeropuertoDestino(Aeropuerto.builder().ciudad("Madrid").codigoIata("MAD").build())
            .fechaSalida(LocalDate.of(2026, 12, 15))
            .horaSalida(LocalTime.of(10, 5))
            .build();

    @Test
    @DisplayName("Describe el vuelo con ruta, fecha y hora")
    void describirVuelo_armaTexto() {
        assertEquals("Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:05",
                NotificadorCancelacionVuelo.describirVuelo(vuelo));
    }

    @Test
    @DisplayName("Manda un mail por aviso y si uno falla sigue con los demás")
    void notificar_unMailFalla_sigueConElResto() {
        VueloCanceladoEvent evento = new VueloCanceladoEvent(vuelo, List.of(
                new VueloCanceladoEvent.Aviso("falla@mail.com", "Ana", "RES1", new BigDecimal("100")),
                new VueloCanceladoEvent.Aviso("ok@mail.com", "Luis", "RES2", BigDecimal.ZERO)));
        doThrow(new RuntimeException("SMTP caído"))
                .when(emailService).sendVueloCanceladoEmail(eq("falla@mail.com"), any(), any(), any(), any());

        assertDoesNotThrow(() -> notificador.notificar(evento));

        verify(emailService).sendVueloCanceladoEmail(eq("ok@mail.com"), eq("Luis"), eq("RES2"),
                eq("Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:05"), eq(BigDecimal.ZERO));
    }
}
