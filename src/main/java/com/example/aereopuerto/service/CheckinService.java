package com.example.aereopuerto.service;

import com.example.aereopuerto.dto.Checkinresponsedto;
import com.example.aereopuerto.dto.Pasajecheckindto;
import com.example.aereopuerto.model.Asiento;
import com.example.aereopuerto.model.Pasaje;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.repository.AsientoRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import com.example.aereopuerto.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class CheckinService {

    private static final Pattern FILA = Pattern.compile("(\\d+)");
    private static final int PRIMERA_FILA_ECONOMICA = 7;

    private final ReservaRepository reservaRepository;
    private final PasajeRepository pasajeRepository;
    private final AsientoRepository asientoRepository;
    private final VueloRepository vueloRepository;

    /**
     * Hace el check-in de todos los pasajes de la reserva.
     * - Si el pasaje ya tiene asiento, se respeta.
     * - Si no tiene, se le asigna uno libre al azar en ESE vuelo.
     * Es idempotente: si ya se hizo, devuelve el resultado sin cambiar nada.
     */
    @Transactional
    // Limpia la caché de reservas: si no, GET /mis-reservas sigue devolviendo la versión sin check-in
    @CacheEvict(value = "reservas", allEntries = true)
    public Checkinresponsedto hacerCheckIn(Integer reservaId, Integer personaId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La reserva no existe"));

        if (!reserva.getPersona().getId().equals(personaId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "La reserva no pertenece al usuario");
        }
        if (reserva.getEstadoReserva() != EstadoReserva.CONFIRMADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Solo se puede hacer check-in de reservas confirmadas (pagas)");
        }

        if (reserva.getPasajes() == null || reserva.getPasajes().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La reserva no tiene pasajes cargados");
        }

        Set<Integer> asignadosAhora = new HashSet<>();

        if (reserva.getFechaCheckIn() == null) {
            // Asientos ocupados por vuelo, cacheados para no repetir asiento
            // entre pasajes de la misma reserva que van en el mismo vuelo
            Map<Integer, Set<Integer>> ocupadosPorVuelo = new HashMap<>();

            for (Pasaje pasaje : reserva.getPasajes()) {
                // Lock sobre el vuelo: dos check-ins simultáneos del mismo vuelo
                // se ejecutan de a uno y no pueden agarrar el mismo asiento
                Vuelo vuelo = vueloRepository.findByIdForUpdate(pasaje.getVuelo().getId())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El vuelo no existe"));

                LocalDateTime salida = LocalDateTime.of(vuelo.getFechaSalida(), vuelo.getHoraSalida());
                if (!salida.isAfter(LocalDateTime.now())) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "El vuelo ya salió, no se puede hacer check-in");
                }

                if (pasaje.getAsiento() != null) {
                    continue; // ya eligió asiento: no se toca
                }

                Set<Integer> ocupados = ocupadosPorVuelo.computeIfAbsent(vuelo.getId(),
                        id -> new HashSet<>(pasajeRepository.findAsientosOcupadosPorVuelo(id, EstadoReserva.CANCELADA)));

                List<Asiento> libres = asientoRepository.findByAvionId(vuelo.getAvion().getId()).stream()
                        .filter(a -> !ocupados.contains(a.getId()))
                        .filter(this::esEconomico) // TODO: elegir según pasaje.getClasesVuelo()
                        .toList();

                if (libres.isEmpty()) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "No quedan asientos libres en el vuelo " + vuelo.getId());
                }

                Asiento elegido = libres.get(ThreadLocalRandom.current().nextInt(libres.size()));
                pasaje.setAsiento(elegido);
                // Mientras el mapa de asientos y crearReserva sigan usando el flag global "ocupado",
                // lo marcamos para que nadie más pueda elegir este asiento
                elegido.setOcupado(true);
                asientoRepository.save(elegido);
                ocupados.add(elegido.getId());
                asignadosAhora.add(pasaje.getId());
            }

            reserva.setFechaCheckIn(LocalDateTime.now());
            // save explícito: si la reserva vino "desconectada" de Hibernate (por ejemplo, desde una caché),
            // sin esto los cambios no se escriben. El cascade ALL guarda también el asiento de cada pasaje.
            reserva = reservaRepository.save(reserva);
        }

        List<Pasajecheckindto> pasajes = reserva.getPasajes().stream()
                .map(p -> new Pasajecheckindto(
                        p.getId(),
                        p.getNombre() + " " + p.getApellido(),
                        p.getVuelo().getId(),
                        p.getAsiento() != null ? p.getAsiento().getCodigo() : null,
                        asignadosAhora.contains(p.getId())))
                .toList();

        return new Checkinresponsedto(reserva.getId(), reserva.getFechaCheckIn(), pasajes);
    }

    private boolean esEconomico(Asiento asiento) {
        if (asiento.getCodigo() == null) return false;
        Matcher m = FILA.matcher(asiento.getCodigo());
        return m.find() && Integer.parseInt(m.group(1)) >= PRIMERA_FILA_ECONOMICA;
    }
}