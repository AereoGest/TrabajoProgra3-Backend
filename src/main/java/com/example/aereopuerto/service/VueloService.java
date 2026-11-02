package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AeropuertoInvalidoException;
import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.Exceptions.VueloInvalidoException;
import com.example.aereopuerto.Specifications.VueloSpecification;
import com.example.aereopuerto.dto.VueloDTO;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoAvion;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.AeropuertoRepository;
import com.example.aereopuerto.repository.AvionRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import com.example.aereopuerto.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VueloService {

    @Autowired
    private final VueloRepository vueloRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final AvionRepository avionRepository;
    private final PasajeRepository pasajeRepository;

    @Cacheable(value = "vuelos", key = "#id")
    public Vuelo obtenerVueloPorId(Integer id) {
        System.out.println("Buscando vuelo " + id);
        return vueloRepository.findById(id)
                .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado. ID: " + id));
    }

    @Caching(put = { @CachePut(value = "vuelos", key = "#result.id") }, evict = {
            @CacheEvict(value = "vuelos", key = "'todos'"),
            @CacheEvict(value = "vuelos", key = "'disponibles'")
    })
    public Vuelo crearVuelo(Vuelo vuelo) {
        validarAvionDisponible(vuelo.getAvion().getId());
        validarDisponibilidadAvion(
                vuelo.getAvion().getId(),
                vuelo.getFechaSalida(),
                vuelo.getFechaLlegada(),
                vuelo.getHoraSalida(),
                vuelo.getHoraLlegada(),
                -1);

        validarDatosVuelo(vuelo);
        validarCiudad(vuelo.getAeropuertoOrigen().getCiudad(), "origen");
        validarCiudad(vuelo.getAeropuertoDestino().getCiudad(), "destino");

        return vueloRepository.save(vuelo);
    }

    @Caching(put = { @CachePut(value = "vuelos", key = "#result.id") }, evict = {
            @CacheEvict(value = "vuelos", key = "'todos'"),
            @CacheEvict(value = "vuelos", key = "'disponibles'")
    })
    public Vuelo actualizarVuelo(Integer id, VueloDTO vueloDTO) {

        Vuelo vuelo = vueloRepository.findById(id)
                .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado. ID: " + id));

        Aeropuerto origen = aeropuertoRepository.findById(vueloDTO.getAeropuertoOrigenId())
                .orElseThrow(() -> new AeropuertoInvalidoException(
                        "Aeropuerto origen no encontrado. ID: " + vueloDTO.getAeropuertoOrigenId()));

        Aeropuerto destino = aeropuertoRepository.findById(vueloDTO.getAeropuertoDestinoId())
                .orElseThrow(() -> new AeropuertoInvalidoException(
                        "Aeropuerto destino no encontrado. ID: " + vueloDTO.getAeropuertoDestinoId()));

        Avion avion = avionRepository.findById(vueloDTO.getAvionId())
                .orElseThrow(() -> new AvionInvalidoException(
                        "Avión no encontrado. ID: " + vueloDTO.getAvionId()));

        validarAvionDisponible(avion.getId());

        vuelo.setAeropuertoOrigen(origen);
        vuelo.setAeropuertoDestino(destino);
        vuelo.setAvion(avion);
        vuelo.setFechaSalida(vueloDTO.getFechaSalida());
        vuelo.setFechaLlegada(vueloDTO.getFechaLlegada());
        vuelo.setHoraSalida(vueloDTO.getHoraSalida());
        vuelo.setHoraLlegada(vueloDTO.getHoraLlegada());
        vuelo.setEstado(vueloDTO.getEstado());
        vuelo.setPrecioVuelo(vueloDTO.getPrecioVuelo());
        vuelo.setEscala(vueloDTO.getEscala());

        validarDisponibilidadAvion(
                vueloDTO.getAvionId(),
                vueloDTO.getFechaSalida(),
                vueloDTO.getFechaLlegada(), // agregás este
                vueloDTO.getHoraSalida(),
                vueloDTO.getHoraLlegada(),
                id);

        validarDatosVuelo(vuelo);

        validarCiudad(vuelo.getAeropuertoOrigen().getCiudad(), "origen");
        validarCiudad(vuelo.getAeropuertoDestino().getCiudad(), "destino");

        return vueloRepository.save(vuelo);
    }

    private void validarDatosVuelo(Vuelo vuelo) {

        if (vuelo.getPrecioVuelo() == null || vuelo.getPrecioVuelo() <= 0) {
            throw new VueloInvalidoException(
                    "El precio del vuelo debe ser mayor a 0.");
        }

        if (vuelo.getAeropuertoOrigen().getId()
                .equals(vuelo.getAeropuertoDestino().getId())) {

            throw new VueloInvalidoException(
                    "El aeropuerto de origen y destino deben ser distintos.");
        }

        if (vuelo.getHoraSalida() == null || vuelo.getHoraLlegada() == null) {
            throw new VueloInvalidoException(
                    "La hora de salida y llegada son obligatorias.");
        }

        if (vuelo.getFechaSalida() == null || vuelo.getFechaLlegada() == null) {
            throw new VueloInvalidoException(
                    "La fecha de salida y llegada son obligatorias.");
        }

        if (vuelo.getFechaLlegada().isBefore(vuelo.getFechaSalida())) {
            throw new VueloInvalidoException(
                    "La fecha de llegada no puede ser anterior a la fecha de salida.");
        }

        if (vuelo.getFechaSalida().equals(vuelo.getFechaLlegada())
                && !vuelo.getHoraLlegada().isAfter(vuelo.getHoraSalida())) {

            throw new VueloInvalidoException(
                    "La hora de llegada debe ser posterior a la hora de salida.");
        }

        LocalDateTime salida = LocalDateTime.of(vuelo.getFechaSalida(), vuelo.getHoraSalida());
        LocalDateTime llegada = LocalDateTime.of(vuelo.getFechaLlegada(), vuelo.getHoraLlegada());
        long horasDuracion = Duration.between(salida, llegada).toHours();

        if (Boolean.FALSE.equals(vuelo.getEscala()) && horasDuracion > 18) {
            throw new VueloInvalidoException(
                    "Un vuelo directo no puede durar más de 18 horas.");
        }

        if (Boolean.TRUE.equals(vuelo.getEscala()) && horasDuracion > 36) {
            throw new VueloInvalidoException(
                    "La duracion total del vuelo no puede exceder las 40 horas.");
        }
    }

    private void validarAvionDisponible(Integer avionId) {
        Avion avion = avionRepository.findById(avionId)
                .orElseThrow(() -> new AvionInvalidoException("Avion no encontrado."));
        if (avion.getEstado() == estadoAvion.MANTENIMIENTO || avion.getEstado() == estadoAvion.BAJA) {
            throw new AvionInvalidoException(
                    "El avion " + avion.getIdentificador() + " no esta disponible (" + avion.getEstado() + "). " +
                            "Selecciona un avion en estado DISPONIBLE o ACTIVO.");
        }
    }

    private void validarDisponibilidadAvion(
            Integer avionId,
            LocalDate fechaSalida,
            LocalDate fechaLlegada,
            LocalTime horaSalida,
            LocalTime horaLlegada,
            Integer excludeId) {

        LocalDateTime salidaDateTime = LocalDateTime.of(fechaSalida, horaSalida);
        LocalDateTime llegadaDateTime = LocalDateTime.of(fechaLlegada, horaLlegada);

        if (!llegadaDateTime.isAfter(salidaDateTime)) {
            throw new VueloInvalidoException(
                    "La fecha y hora de llegada debe ser posterior a la fecha y hora de salida.");
        }

        boolean tieneConflicto = vueloRepository.existeConflictoHorario(
                avionId,
                fechaSalida,
                horaSalida,
                fechaLlegada,
                horaLlegada,
                excludeId);

        if (tieneConflicto) {
            throw new AvionInvalidoException(
                    "El avión ya tiene un vuelo asignado en ese horario.");
        }
    }

    @Caching(evict = {
            @CacheEvict(value = "vuelos", key = "#id"),
            @CacheEvict(value = "vuelos", key = "'todos'"),
            @CacheEvict(value = "vuelos", key = "'disponibles'")
    })
    public void eliminarVuelo(Integer id) {
        System.out.println("Eliminando vuelo " + id);
        vueloRepository.deleteById(id);
    }

    @Cacheable(value = "vuelos", key = "'todos'")
    public List<Vuelo> obtenerTodosLosVuelos() {
        return vueloRepository.findAll();
    }

    @Cacheable(value = "vuelos", key = "'disponibles'")
    public List<Vuelo> obtenerVuelosCliente() {
        return vueloRepository.findByEstadoNot(estadoVuelo.CANCELADO);
    }

    @Caching(evict = {
            @CacheEvict(value = "vuelos", key = "'todos'"),
            @CacheEvict(value = "vuelos", key = "'disponibles'")
    })
    public void invalidarListaDeVuelos() {
        System.out.println("Limpiando la cache");
    }

    public List<Vuelo> buscarVuelosConFiltrosAvanzados(
            String origen,
            String destino,
            LocalDateTime fechaSalida,
            LocalDateTime fechaLlegada,
            Double precioMaximo,
            Boolean escala,
            estadoVuelo estado) {

        List<Vuelo> vuelos = buscarVuelos(origen, destino, fechaSalida, fechaLlegada, precioMaximo, escala, estado);
        return filtrarConCapacidad(vuelos);
    }

    private List<Vuelo> filtrarConCapacidad(List<Vuelo> vuelos) {
        if (vuelos.isEmpty())
            return vuelos;
        List<Integer> ids = vuelos.stream().map(Vuelo::getId).toList();
        List<Object[]> counts = pasajeRepository.countGroupedByVueloIdIn(ids);
        Map<Integer, Long> vendidosMap = new HashMap<>();
        for (Object[] row : counts) {
            vendidosMap.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        return vuelos.stream()
                .filter(v -> {
                    int capacidad = v.getAvion().getCapacidadPasajeros();
                    long vendidos = vendidosMap.getOrDefault(v.getId(), 0L);
                    return vendidos < capacidad;
                })
                .toList();
    }

    public List<Vuelo> buscarVuelosCliente(
            String origen,
            String destino,
            LocalDateTime fechaSalida,
            LocalDateTime fechaLlegada,
            Double precioMaximo,
            Boolean escala) {

        return buscarVuelos(origen, destino, fechaSalida, fechaLlegada, precioMaximo, escala, null);
    }

    private List<Vuelo> buscarVuelos(
            String origen,
            String destino,
            LocalDateTime fechaSalida,
            LocalDateTime fechaLlegada,
            Double precioMaximo,
            Boolean escala,
            estadoVuelo estado) {

        validarCiudad(origen, "origen");
        validarCiudad(destino, "destino");

        Specification<Vuelo> spec = Specification
                .where(VueloSpecification.noCancelados())
                .and(VueloSpecification.porOrigenGeneral(origen))
                .and(VueloSpecification.porDestinoGeneral(destino))
                .and(VueloSpecification.porFechaSalidaDesde(fechaSalida))
                .and(VueloSpecification.porFechaLlegadaHasta(fechaLlegada))
                .and(VueloSpecification.precioMenorOIgualA(precioMaximo))
                .and(VueloSpecification.porEscala(escala))
                .and(VueloSpecification.porEstado(estado));

        return vueloRepository.findAll(spec);
    }

    private void validarCiudad(String ciudad, String campo) {
        if (ciudad == null) return;

        if (ciudad.isBlank()) {
            throw new VueloInvalidoException("El " + campo + " no puede estar vacío.");
        }

        if (!ciudad.matches("^[\\p{L}\\s]+$")) {
            throw new VueloInvalidoException(
                    "El " + campo + " solo puede contener letras y espacios.");
        }
    }
}