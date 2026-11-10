package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.service.EmailService;
import com.example.aereopuerto.model.Vuelo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Avisa por mail a los clientes cuyas reservas se cancelaron porque se canceló el vuelo.
 * Corre después de que la cancelación se guardó: si un mail falla, la cancelación ya está hecha
 * y el error solo se registra en el log.
 */
@Component
@RequiredArgsConstructor
public class NotificadorCancelacionVuelo {

    private static final Logger log = LoggerFactory.getLogger(NotificadorCancelacionVuelo.class);

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notificar(VueloCanceladoEvent evento) {
        String descripcion = describirVuelo(evento.vuelo());
        for (VueloCanceladoEvent.Aviso aviso : evento.avisos()) {
            try {
                emailService.sendVueloCanceladoEmail(
                        aviso.email(), aviso.nombre(), aviso.codigoReserva(), descripcion, aviso.montoReembolso());
            } catch (Exception e) {
                log.warn("No se pudo avisar la cancelación de la reserva {} a {}: {}",
                        aviso.codigoReserva(), aviso.email(), e.getMessage());
            }
        }
    }

    /** Ej.: "Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:00". */
    static String describirVuelo(Vuelo vuelo) {
        return DescripcionVuelo.de(vuelo);
    }
}
