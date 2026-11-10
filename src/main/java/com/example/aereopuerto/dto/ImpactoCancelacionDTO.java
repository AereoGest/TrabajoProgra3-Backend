package com.example.aereopuerto.dto;

import java.math.BigDecimal;

/**
 * Qué pasaría (o pasó) con los clientes al cancelar un vuelo.
 *
 * @param reservas        reservas no canceladas que tiene el vuelo
 * @param pasajeros       pasajes incluidos en esas reservas
 * @param montoReembolso  total a devolver (100% de lo pagado)
 */
public record ImpactoCancelacionDTO(int reservas, int pasajeros, BigDecimal montoReembolso) {
}
