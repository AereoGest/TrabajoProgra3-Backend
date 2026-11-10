package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.dto.ImpactoCancelacionDTO;
import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.repository.AsientoRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Qué pasa con los clientes cuando la aerolínea cancela un vuelo:
 * sus reservas se cancelan, se liberan los asientos, se registra el reembolso
 * del 100% de lo pagado y se les avisa por mail.
 */
@Service
@RequiredArgsConstructor
public class CancelacionVueloService {

    public static final String MOTIVO_VUELO_CANCELADO = "Vuelo cancelado por la aerolínea";

    private final ReservaRepository reservaRepository;
    private final AsientoRepository asientoRepository;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;

    /** Cuántas reservas y pasajeros se verían afectados si se cancela el vuelo (no modifica nada). */
    public ImpactoCancelacionDTO calcularImpacto(Integer vueloId) {
        List<Reserva> reservas = reservaRepository.findNoCanceladasPorVuelo(vueloId);
        int pasajeros = reservas.stream().mapToInt(CancelacionVueloService::cantidadPasajeros).sum();
        BigDecimal monto = reservas.stream()
                .map(CancelacionVueloService::reembolsoTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ImpactoCancelacionDTO(reservas.size(), pasajeros, monto);
    }

    /**
     * Cancela todas las reservas activas del vuelo. Se ejecuta dentro de la misma transacción
     * que pasa el vuelo a CANCELADO: si algo falla, no queda nada a medias.
     */
    @Transactional
    @CacheEvict(value = "reservas", allEntries = true)
    public ImpactoCancelacionDTO cancelarReservasDelVuelo(Vuelo vuelo) {
        List<Reserva> reservas = reservaRepository.findNoCanceladasPorVuelo(vuelo.getId());
        List<VueloCanceladoEvent.Aviso> avisos = new ArrayList<>();
        int pasajeros = 0;
        BigDecimal total = BigDecimal.ZERO;

        for (Reserva reserva : reservas) {
            BigDecimal reembolso = reembolsoTotal(reserva);
            liberarAsientos(reserva);
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            reserva.setMontoReembolso(reembolso);
            reserva.setMotivoCancelacion(MOTIVO_VUELO_CANCELADO);

            pasajeros += cantidadPasajeros(reserva);
            total = total.add(reembolso);

            String email = emailDelTitular(reserva);
            if (email != null) {
                String nombre = reserva.getPersona() != null ? reserva.getPersona().getNombre() : null;
                avisos.add(new VueloCanceladoEvent.Aviso(email, nombre, reserva.getCodigoReserva(), reembolso));
            }
        }

        if (!reservas.isEmpty()) {
            reservaRepository.saveAll(reservas);
        }
        if (!avisos.isEmpty()) {
            // Los mails salen recién cuando se confirma la transacción
            eventPublisher.publishEvent(new VueloCanceladoEvent(vuelo, avisos));
        }
        return new ImpactoCancelacionDTO(reservas.size(), pasajeros, total);
    }

    /**
     * Cancela la aerolínea: se devuelve el 100% de lo pagado. Una reserva que todavía no se pagó
     * (PROCESANDO) no tiene nada que devolver.
     */
    static BigDecimal reembolsoTotal(Reserva reserva) {
        boolean pagada = reserva.getEstadoReserva() == EstadoReserva.CONFIRMADO
                || reserva.getEstadoReserva() == EstadoReserva.ACTIVO;
        return pagada && reserva.getValor() != null ? reserva.getValor() : BigDecimal.ZERO;
    }

    private static int cantidadPasajeros(Reserva reserva) {
        if (reserva.getPasajes() != null && !reserva.getPasajes().isEmpty()) return reserva.getPasajes().size();
        return reserva.getCantidadPasajes() != null ? reserva.getCantidadPasajes() : 0;
    }

    private void liberarAsientos(Reserva reserva) {
        if (reserva.getPasajes() == null) return;
        for (Pasaje pasaje : reserva.getPasajes()) {
            if (pasaje.getAsiento() != null) {
                pasaje.getAsiento().setOcupado(false);
                asientoRepository.save(pasaje.getAsiento());
                pasaje.setAsiento(null);
            }
        }
    }

    private String emailDelTitular(Reserva reserva) {
        if (reserva.getPersona() == null || reserva.getPersona().getId() == null) return null;
        return userRepository.findByPersonaId(reserva.getPersona().getId())
                .map(User::getEmail)
                .orElse(null);
    }
}
