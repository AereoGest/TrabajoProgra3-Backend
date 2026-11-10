package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.service.EmailService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificadorCompraConfirmadaTest {

    @Mock
    private EmailService emailService;

    @InjectMocks
    private NotificadorCompraConfirmada notificador;

    private final CompraConfirmadaEvent evento = new CompraConfirmadaEvent(
            "juan@aerogest.com", "Juan", "AC-2026-PAGO", "Vuelo", List.of(),
            new BigDecimal("150000"), 1, "Mercado Pago");

    @Test
    @DisplayName("Manda el recibo por mail")
    void notificar_mandaRecibo() {
        notificador.notificar(evento);

        verify(emailService).sendReciboCompraEmail(evento);
    }

    @Test
    @DisplayName("Si el mail falla no rompe la compra")
    void notificar_mailFalla_noPropagaError() {
        doThrow(new RuntimeException("SMTP caído")).when(emailService).sendReciboCompraEmail(evento);

        assertDoesNotThrow(() -> notificador.notificar(evento));
    }
}
