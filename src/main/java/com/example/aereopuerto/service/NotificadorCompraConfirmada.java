package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Manda el recibo de compra por mail cuando se aprueba el pago de una reserva.
 * Corre después de que el pago y la factura se guardaron: si el mail falla, la compra
 * ya está hecha y el error solo se registra en el log.
 */
@Component
@RequiredArgsConstructor
public class NotificadorCompraConfirmada {

    private static final Logger log = LoggerFactory.getLogger(NotificadorCompraConfirmada.class);

    private final EmailService emailService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void notificar(CompraConfirmadaEvent evento) {
        try {
            emailService.sendReciboCompraEmail(evento);
        } catch (Exception e) {
            log.warn("No se pudo mandar el recibo de la reserva {} a {}: {}",
                    evento.codigoReserva(), evento.email(), e.getMessage());
        }
    }
}
