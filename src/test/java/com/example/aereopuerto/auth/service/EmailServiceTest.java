package com.example.aereopuerto.auth.service;

import com.example.aereopuerto.service.CompraConfirmadaEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "frontendUrl", "https://aerogest.test");
    }

    private CompraConfirmadaEvent recibo(String nombre, Integer cuotas) {
        return new CompraConfirmadaEvent(
                "juan@aerogest.com", nombre, "AC-2026-PAGO",
                "Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:05",
                List.of(new CompraConfirmadaEvent.Pasajero("Juan Pérez", "Económica", "12A"),
                        new CompraConfirmadaEvent.Pasajero("Ana Gómez", "Económica", null)),
                new BigDecimal("150000"), cuotas, "Tarjeta de crédito");
    }

    @Test
    @DisplayName("El recibo va al mail del cliente con la reserva, el vuelo, los pasajeros y el total")
    void sendReciboCompraEmail_armaElMail() {
        emailService.sendReciboCompraEmail(recibo("Juan", 3));

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage mail = captor.getValue();

        assertThat(mail.getTo()).containsExactly("juan@aerogest.com");
        assertThat(mail.getSubject()).isEqualTo("Recibo de tu compra - Reserva AC-2026-PAGO");
        assertThat(mail.getText())
                .startsWith("Hola Juan,")
                .contains("Reserva: AC-2026-PAGO")
                .contains("Vuelo: Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:05")
                .contains("Juan Pérez · Económica · Asiento 12A")
                .contains("Ana Gómez · Económica · Asiento a asignar en el check-in")
                .contains("Total pagado: ")
                .contains("150.000")
                .contains("Medio de pago: Tarjeta de crédito en 3 cuotas")
                .contains("https://aerogest.test/reservas");
    }

    @Test
    @DisplayName("Con nombre pendiente saluda genérico y en un pago no menciona cuotas")
    void armarTextoRecibo_nombrePendienteYUnPago() {
        String texto = emailService.armarTextoRecibo(recibo("Pendiente", 1));

        assertThat(texto).startsWith("Hola,\n\n");
        assertThat(texto).contains("Medio de pago: Tarjeta de crédito\n");
        assertThat(texto).doesNotContain("cuotas");
    }
}
