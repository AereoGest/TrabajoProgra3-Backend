package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.*;
import com.example.aereopuerto.dto.PasajeDTO;
import com.example.aereopuerto.model.*;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PasajeService {
    @Autowired
    private final PasajeRepository pasajeRepository;
    private final VueloRepository vueloRepository;
    private final EquipajeRepository equipajeRepository;
    private final ReservaRepository reservaRepository;
    private final AsientoRepository asientoRepository;
    private final AsistenciaRepository asistenciaRepository;

    @Value("${tarifas.impuesto:0.21}")
    private BigDecimal tasaImpuesto;

    @Value("${tarifas.servicio:0.05}")
    private BigDecimal tasaServicio;

    @Cacheable(value = "pasajes", key = "#id")
    public Pasaje obtenerPasajePorId(Integer id) {
        return pasajeRepository.findById(id)
                .orElseThrow(() -> new PasajeInvalidoException("Pasaje no encontrado. ID: " + id));
    }

    @Cacheable(value = "pasajes", key = "'todos'")
    public List<Pasaje> obtenerTodasLosPasajes() {
        return pasajeRepository.findAll();
    }

    public List<Pasaje> obtenerPasajesPorReserva(Integer reservaId) {
        reservaRepository.findById(reservaId)
                .orElseThrow(() -> new ReservaInvalidaException("Reserva no encontrada. ID: " + reservaId));
        return pasajeRepository.findByReservaId(reservaId);
    }

    @CachePut(value = "pasajes", key = "#result.id")
    @CacheEvict(value = "pasajes", key = "'todos'")
    public Pasaje crearPasaje(Pasaje pasaje) {
        return pasajeRepository.save(pasaje);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "pasajes", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "pasajes", key = "'todos'"),
                    @CacheEvict(value = "reservas", allEntries = true)
            })
    public Pasaje editarPasaje(Integer id, PasajeDTO pasajeDTO) {
        if (pasajeDTO == null) {
            throw new ReservaInvalidaException("Los datos del pasaje son obligatorios");
        }

        Pasaje pasajeExistente = pasajeRepository.findById(id)
                .orElseThrow(() -> new PasajeInvalidoException("Pasaje no encontrado. ID: " + id));

        Reserva reserva = pasajeExistente.getReserva();
        if (pasajeDTO.getReservaId() != null) {
            if (reserva == null || !Objects.equals(reserva.getId(), pasajeDTO.getReservaId())) {
                throw new ReservaInvalidaException(
                        "El pasaje " + id + " no pertenece a la reserva indicada (" + pasajeDTO.getReservaId() + ")");
            }
        }
        if (reserva == null) {
            throw new ReservaInvalidaException("El pasaje no tiene una reserva asociada");
        }
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new ReservaInvalidaException("No se puede modificar un pasaje de una reserva cancelada");
        }

        Vuelo vuelo = pasajeExistente.getVuelo();
        if (pasajeDTO.getVueloId() != null && (vuelo == null || !Objects.equals(vuelo.getId(), pasajeDTO.getVueloId()))) {
            vuelo = vueloRepository.findById(pasajeDTO.getVueloId())
                    .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado. ID: " + pasajeDTO.getVueloId()));
        }
        if (vuelo == null) {
            throw new VueloInvalidoException("El pasaje no tiene un vuelo válido asociado");
        }

        validarDatosPasajero(pasajeDTO, pasajeExistente, reserva);

        // Gestión del cambio de asiento
        Asiento asientoAnterior = pasajeExistente.getAsiento();
        Asiento nuevoAsiento = null;

        if (pasajeDTO.getAsientoId() != null) {
            if (asientoAnterior != null && Objects.equals(asientoAnterior.getId(), pasajeDTO.getAsientoId())) {
                nuevoAsiento = asientoAnterior;
            } else {
                nuevoAsiento = asientoRepository.findById(pasajeDTO.getAsientoId())
                        .orElseThrow(() -> new AsientoInvalidoException(
                                "Asiento no encontrado. ID: " + pasajeDTO.getAsientoId()));

                if (nuevoAsiento.getAvion() == null || vuelo.getAvion() == null
                        || !Objects.equals(nuevoAsiento.getAvion().getId(), vuelo.getAvion().getId())) {
                    throw new ReservaInvalidaException(
                            "El asiento " + nuevoAsiento.getCodigo() + " no pertenece al avión de este vuelo");
                }
                if (Boolean.TRUE.equals(nuevoAsiento.getOcupado())) {
                    throw new ReservaInvalidaException(
                            "El asiento " + nuevoAsiento.getCodigo() + " ya se encuentra ocupado");
                }

                if (asientoAnterior != null) {
                    asientoAnterior.setOcupado(false);
                    asientoRepository.save(asientoAnterior);
                }
                nuevoAsiento.setOcupado(true);
                asientoRepository.save(nuevoAsiento);
            }
        } else if (asientoAnterior != null) {
            asientoAnterior.setOcupado(false);
            asientoRepository.save(asientoAnterior);
        }

        // Gestión de equipajes
        List<Equipaje> equipajes = new ArrayList<>();
        if (pasajeDTO.getEquipajeIds() != null) {
            List<Integer> idsUnicos = pasajeDTO.getEquipajeIds().stream()
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            for (Integer eId : idsUnicos) {
                equipajes.add(equipajeRepository.findById(eId)
                        .orElseThrow(() -> new EquipajeInvalidoException("Equipaje no encontrado. ID: " + eId)));
            }
        }

        // Gestión de asistencia al viajero
        AsistenciaAlViajero asistencia = null;
        if (pasajeDTO.getAsistenciaId() != null) {
            asistencia = asistenciaRepository.findById(pasajeDTO.getAsistenciaId())
                    .orElseThrow(() -> new AsistenciaInvalidaException(
                            "Asistencia no encontrada. ID: " + pasajeDTO.getAsistenciaId()));
        }

        pasajeExistente.setNombre(pasajeDTO.getNombre().trim());
        pasajeExistente.setApellido(pasajeDTO.getApellido().trim());
        if (pasajeDTO.getTipoDocumento() != null) {
            pasajeExistente.setTipoDocumento(pasajeDTO.getTipoDocumento());
        }
        pasajeExistente.setDocumento(normalizarDocumento(pasajeDTO.getDocumento()));
        if (pasajeDTO.getFechaNacimiento() != null) {
            pasajeExistente.setFechaNacimiento(pasajeDTO.getFechaNacimiento());
        }
        pasajeExistente.setEmail(
                pasajeDTO.getEmail() == null || pasajeDTO.getEmail().isBlank()
                        ? null
                        : pasajeDTO.getEmail().trim());

        if (pasajeDTO.getCodigoPasaje() != null && !pasajeDTO.getCodigoPasaje().isBlank()) {
            pasajeExistente.setCodigoPasaje(pasajeDTO.getCodigoPasaje().trim());
        }
        if (pasajeDTO.getClasesVuelo() != null) {
            pasajeExistente.setClasesVuelo(pasajeDTO.getClasesVuelo());
        }
        pasajeExistente.setVuelo(vuelo);
        pasajeExistente.setAsiento(nuevoAsiento);
        pasajeExistente.setEquipajes(equipajes);
        pasajeExistente.setAsistenciaAlViajero(asistencia);

        Pasaje guardado = pasajeRepository.save(pasajeExistente);
        recalcularYGuardarReserva(reserva);
        return guardado;
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "pasajes", key = "#id"),
            @CacheEvict(value = "pasajes", key = "'todos'"),
            @CacheEvict(value = "reservas", allEntries = true)
    })
    public void eliminarPasaje(Integer id) {
        cancelarPasajeDeReserva(id, null);
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "pasajes", key = "#id"),
            @CacheEvict(value = "pasajes", key = "'todos'"),
            @CacheEvict(value = "reservas", allEntries = true)
    })
    public void cancelarPasajeDeReserva(Integer id, Integer reservaIdEsperada) {
        Pasaje pasaje = pasajeRepository.findById(id)
                .orElseThrow(() -> new PasajeInvalidoException("Pasaje no encontrado. ID: " + id));

        Reserva reserva = pasaje.getReserva();
        if (reserva == null) {
            throw new ReservaInvalidaException("El pasaje no tiene una reserva asociada");
        }
        if (reservaIdEsperada != null && !Objects.equals(reserva.getId(), reservaIdEsperada)) {
            throw new ReservaInvalidaException(
                    "El pasaje " + id + " no pertenece a la reserva indicada (" + reservaIdEsperada + ")");
        }
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new ReservaInvalidaException("No se puede cancelar un pasaje de una reserva que ya está cancelada");
        }

        if (pasaje.getAsiento() != null) {
            Asiento asiento = pasaje.getAsiento();
            asiento.setOcupado(false);
            asientoRepository.save(asiento);
            pasaje.setAsiento(null);
        }

        if (reserva.getPasajes() != null) {
            reserva.getPasajes().removeIf(p -> Objects.equals(p.getId(), pasaje.getId()));
        }
        pasajeRepository.delete(pasaje);

        if (reserva.getPasajes() == null || reserva.getPasajes().isEmpty()) {
            reserva.setCantidadPasajes(0);
            reserva.setValor(BigDecimal.ZERO);
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
        } else {
            recalcularYGuardarReserva(reserva);
        }
    }

    private void validarDatosPasajero(PasajeDTO dto, Pasaje pasajeActual, Reserva reserva) {
        if (dto.getNombre() == null || dto.getNombre().isBlank()
                || dto.getApellido() == null || dto.getApellido().isBlank()) {
            throw new ReservaInvalidaException("El nombre y el apellido del pasajero son obligatorios");
        }
        if (dto.getClasesVuelo() == null) {
            throw new ReservaInvalidaException("La clase de vuelo es obligatoria");
        }

        Identificador tipoDoc = dto.getTipoDocumento() != null
                ? dto.getTipoDocumento()
                : (pasajeActual.getTipoDocumento() != null ? pasajeActual.getTipoDocumento() : Identificador.DNI);

        if (dto.getDocumento() == null || dto.getDocumento().isBlank()) {
            throw new ReservaInvalidaException("El número de documento del pasajero es obligatorio");
        }
        String docNorm = normalizarDocumento(dto.getDocumento());
        boolean docValido = switch (tipoDoc) {
            case DNI -> docNorm.matches("\\d{7,8}");
            case PASAPORTE -> docNorm.matches("[A-Z0-9]{6,20}");
        };
        if (!docValido) {
            throw new ReservaInvalidaException("El documento no es válido para el tipo " + tipoDoc);
        }

        if (dto.getFechaNacimiento() != null && !dto.getFechaNacimiento().isBefore(LocalDate.now())) {
            throw new ReservaInvalidaException("La fecha de nacimiento debe ser anterior a hoy");
        }

        if (reserva.getPasajes() != null) {
            for (Pasaje otro : reserva.getPasajes()) {
                if (Objects.equals(otro.getId(), pasajeActual.getId())) {
                    continue;
                }
                Identificador otroTipo = otro.getTipoDocumento() != null ? otro.getTipoDocumento() : Identificador.DNI;
                String otroDoc = otro.getDocumento() != null ? normalizarDocumento(otro.getDocumento()) : "";
                if (otroTipo == tipoDoc && otroDoc.equals(docNorm)) {
                    throw new ReservaInvalidaException(
                            "El documento " + docNorm + " ya pertenece a otro pasajero en la misma reserva");
                }
            }
        }
    }

    private String normalizarDocumento(String doc) {
        return doc.replaceAll("[\\s.\\-]", "").toUpperCase();
    }

    private void recalcularYGuardarReserva(Reserva reserva) {
        List<Pasaje> pasajes = reserva.getPasajes() != null ? reserva.getPasajes() : List.of();
        reserva.setCantidadPasajes(pasajes.size());

        if (pasajes.isEmpty()) {
            reserva.setValor(BigDecimal.ZERO);
            reserva.setEstadoReserva(EstadoReserva.CANCELADA);
            reservaRepository.save(reserva);
            return;
        }

        BigDecimal base = BigDecimal.ZERO;
        BigDecimal extras = BigDecimal.ZERO;

        for (Pasaje p : pasajes) {
            if (p.getVuelo() != null && p.getVuelo().getPrecioVuelo() != null) {
                base = base.add(BigDecimal.valueOf(p.getVuelo().getPrecioVuelo()));
            }
            if (p.getAsiento() != null && p.getAsiento().getPrecioExtra() != null) {
                extras = extras.add(BigDecimal.valueOf(p.getAsiento().getPrecioExtra()));
            }
            if (p.getEquipajes() != null) {
                for (Equipaje eq : p.getEquipajes()) {
                    if (eq.getPrecio() != null) {
                        extras = extras.add(BigDecimal.valueOf(eq.getPrecio()));
                    }
                }
            }
            if (p.getAsistenciaAlViajero() != null && p.getAsistenciaAlViajero().getPrecio() != null) {
                extras = extras.add(BigDecimal.valueOf(p.getAsistenciaAlViajero().getPrecio()));
            }
        }

        BigDecimal imp = tasaImpuesto != null ? tasaImpuesto : new BigDecimal("0.21");
        BigDecimal serv = tasaServicio != null ? tasaServicio : new BigDecimal("0.05");

        BigDecimal impuestos = base.multiply(imp).setScale(0, RoundingMode.HALF_UP);
        BigDecimal subtotal = base.add(extras).add(impuestos);
        BigDecimal servicio = subtotal.multiply(serv).setScale(0, RoundingMode.HALF_UP);
        reserva.setValor(subtotal.add(servicio));

        reservaRepository.save(reserva);
    }

    @CacheEvict(value = "pasajes", key = "'todos'")
    public void invalidarListaDePasajes() {
    }
}
