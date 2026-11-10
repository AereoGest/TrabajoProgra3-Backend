package com.example.aereopuerto.service;

import com.example.aereopuerto.model.Vuelo;

import java.math.BigDecimal;
import java.util.List;

/**
 * Se publica cuando la aerolínea cancela un vuelo y se cancelan sus reservas.
 * Los mails se mandan recién cuando la transacción se confirma (ver {@link NotificadorCancelacionVuelo}).
 */
public record VueloCanceladoEvent(Vuelo vuelo, List<Aviso> avisos) {

    /** Datos para avisarle a un cliente que su reserva se canceló. */
    public record Aviso(String email, String nombre, String codigoReserva, BigDecimal montoReembolso) {
    }
}
