package com.example.aereopuerto.service;

import com.example.aereopuerto.dto.ReembolsoDTO;
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
 * Cuánto se le devuelve al cliente cuando se cancela su reserva.
 * <p>
 * Si cancela el cliente (o un empleado), según los días que faltan para la salida del vuelo:
 * <ul>
 *   <li>30 días o más: 100%</li>
 *   <li>20 a 29 días: 50%</li>
 *   <li>11 a 19 días: 15%</li>
 *   <li>10 días o menos (o el vuelo ya salió): nada</li>
 * </ul>
 * Si cancela un admin, se devuelve siempre el 100% ({@link #simularTotal}).
 * Si cancela la aerolínea el vuelo entero, también el 100% (ver {@link CancelacionVueloService}).
 * Una reserva que no se pagó no tiene nada que devolver.
 */
@Service
public class PoliticaDevolucionService {

    private static final BigDecimal CIEN = new BigDecimal("100");
    private static final BigDecimal CINCUENTA_POR_CIENTO = new BigDecimal("0.50");
    private static final BigDecimal QUINCE_POR_CIENTO = new BigDecimal("0.15");

    // ===================== Cálculo =====================

    /** Monto a devolver según la política de días (cancela el cliente o un empleado). */
    public BigDecimal calcular(Reserva reserva) {
        LocalDate salida = fechaSalidaDelVuelo(reserva);
        if (salida == null || reserva.getValor() == null || !estaPagada(reserva)) {
            return BigDecimal.ZERO;
        }

        // Lo que importa es cuánto falta para que salga el vuelo (no la fecha en que se reservó).
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

    // ===================== Simulación (para mostrar antes de cancelar) =====================

    /**
     * Cuánto se devolvería si el cliente cancelara hoy, según la política de días (no modifica nada).
     * Si la reserva ya está cancelada, devuelve el monto que quedó registrado.
     */
    public ReembolsoDTO simular(Reserva reserva) {
        BigDecimal valor = valorDe(reserva);
        Long diasRestantes = diasHastaSalida(reserva);

        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            BigDecimal registrado = reserva.getMontoReembolso() != null ? reserva.getMontoReembolso() : BigDecimal.ZERO;
            return new ReembolsoDTO(reserva.getId(), valor, porcentajeDe(registrado, valor),
                    registrado, diasRestantes, true, true);
        }

        BigDecimal monto = calcular(reserva);
        return new ReembolsoDTO(reserva.getId(), valor, porcentajeDe(monto, valor),
                monto, diasRestantes, estaPagada(reserva), false);
    }

    /**
     * Cuánto se devolvería si cancela un admin: todo lo pagado (si la reserva estaba pagada).
     * Si la reserva ya está cancelada, devuelve el monto que quedó registrado.
     */
    public ReembolsoDTO simularTotal(Reserva reserva) {
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            return simular(reserva);
        }
        BigDecimal valor = valorDe(reserva);
        boolean pagada = estaPagada(reserva);
        return new ReembolsoDTO(reserva.getId(), valor, pagada ? 100 : 0,
                pagada ? valor : BigDecimal.ZERO, diasHastaSalida(reserva), pagada, false);
    }

    // ===================== Auxiliares =====================

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

    /** Días que faltan para la salida del vuelo (null si la reserva no tiene vuelo). */
    private static Long diasHastaSalida(Reserva reserva) {
        LocalDate salida = fechaSalidaDelVuelo(reserva);
        return salida != null ? ChronoUnit.DAYS.between(LocalDate.now(), salida) : null;
    }

    private static boolean estaPagada(Reserva reserva) {
        return reserva.getEstadoReserva() == EstadoReserva.CONFIRMADO
                || reserva.getEstadoReserva() == EstadoReserva.ACTIVO;
    }

    private static BigDecimal valorDe(Reserva reserva) {
        return reserva.getValor() != null ? reserva.getValor() : BigDecimal.ZERO;
    }

    private static BigDecimal porcentaje(BigDecimal valor, BigDecimal porcentaje) {
        return valor.multiply(porcentaje).setScale(2, RoundingMode.HALF_UP);
    }

    /** Qué porcentaje del valor representa el monto (ej. 100000 de 200000 -> 50). */
    private static int porcentajeDe(BigDecimal monto, BigDecimal valor) {
        return valor.signum() > 0
                ? monto.multiply(CIEN).divide(valor, 0, RoundingMode.HALF_UP).intValue()
                : 0;
    }
}