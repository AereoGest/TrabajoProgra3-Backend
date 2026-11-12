package com.example.aereopuerto.service;

import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Cuánto se le devuelve al cliente cuando él cancela su reserva, según los días
 * que faltan para la salida del vuelo:
 * <ul>
 *   <li>30 días o más: 100%</li>
 *   <li>20 a 29 días: 50%</li>
 *   <li>11 a 19 días: 15%</li>
 *   <li>10 días o menos (o el vuelo ya salió): nada</li>
 * </ul>
 * Una reserva que no se pagó no tiene nada que devolver.
 * (Si cancela la aerolínea, se devuelve siempre el 100%: ver {@link CancelacionVueloService}.)
 */
@Service
public class PoliticaDevolucionService {

    private static final BigDecimal CINCUENTA_POR_CIENTO = new BigDecimal("0.50");
    private static final BigDecimal QUINCE_POR_CIENTO = new BigDecimal("0.15");

    public BigDecimal calcular(Reserva reserva) {
        LocalDate salida = fechaSalidaDelVuelo(reserva);
        if (salida == null || reserva.getValor() == null || !estaPagada(reserva)) {
            return BigDecimal.ZERO;
        }

        // Antes se contaban los días hasta la fecha en que se hizo la reserva (siempre 0 o negativo),
        // así que el reembolso daba siempre 0. Lo que importa es cuánto falta para que salga el vuelo.
        long diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), salida);

        if (diasRestantes >= 30) {
            return reserva.getValor();
        } else if (diasRestantes >= 20) {
            return porcentaje(reserva.getValor(), CINCUENTA_POR_CIENTO);
        } else if (diasRestantes > 10) {
            return porcentaje(reserva.getValor(), QUINCE_POR_CIENTO);
        }
        return BigDecimal.ZERO;
    }

    /** Fecha de salida del vuelo de la reserva (todos sus pasajes son del mismo vuelo). */
    private static LocalDate fechaSalidaDelVuelo(Reserva reserva) {
        List<Pasaje> pasajes = reserva.getPasajes();
        if (pasajes == null) return null;
        return pasajes.stream()
                .map(Pasaje::getVuelo)
                .filter(Objects::nonNull)
                .map(Vuelo::getFechaSalida)
                .filter(Objects::nonNull)
                .min(Comparator.naturalOrder())
                .orElse(null);
    }

    private static boolean estaPagada(Reserva reserva) {
        return reserva.getEstadoReserva() == EstadoReserva.CONFIRMADO
                || reserva.getEstadoReserva() == EstadoReserva.ACTIVO;
    }

    private static BigDecimal porcentaje(BigDecimal valor, BigDecimal porcentaje) {
        return valor.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
    }
}
