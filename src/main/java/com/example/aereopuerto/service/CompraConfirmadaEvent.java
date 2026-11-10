package com.example.aereopuerto.service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Se publica cuando Mercado Pago aprueba el pago de una reserva.
 * El recibo se manda recién cuando la transacción se confirma (ver {@link NotificadorCompraConfirmada}).
 * Lleva los datos ya armados como texto para no tocar entidades JPA fuera de la transacción.
 */
public record CompraConfirmadaEvent(
        String email,
        String nombre,
        String codigoReserva,
        String descripcionVuelo,
        List<Pasajero> pasajeros,
        BigDecimal total,
        Integer cuotas,
        String medioDePago) {

    /** Un renglón del recibo por cada pasaje comprado. */
    public record Pasajero(String nombreCompleto, String clase, String asiento) {
    }
}
