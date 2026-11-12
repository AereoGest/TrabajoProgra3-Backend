package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AeropuertoInvalidoException;
import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.Exceptions.VueloInvalidoException;
import com.example.aereopuerto.Specifications.VueloSpecification;
import com.example.aereopuerto.dto.ValidacionVueloDTO;
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
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VueloService {

    @Autowired
    private final VueloRepository vueloRepository;
    private final AeropuertoRepository aeropuertoRepository;
    private final AvionRepository avionRepository;
    private final PasajeRepository pasajeRepository;
    private final CancelacionVueloService cancelacionVueloService;

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
    public Vuelo crearVuelo(VueloDTO vueloDTO) {
        Vuelo vuelo = new Vuelo();
        prepararAlta(vuelo, vueloDTO);
        return vueloRepository.save(vuelo);
    }

    @Caching(put = { @CachePut(value = "vuelos", key = "#result.id") }, evict = {
            @CacheEvict(value = "vuelos", key = "'todos'"),
            @CacheEvict(value = "vuelos", key = "'disponibles'")
    })
    @Transactional
    public Vuelo actualizarVuelo(Integer id, VueloDTO vueloDTO) {
        Vuelo vuelo = buscarVuelo(id);
        validarNoFinalizado(vuelo, "modificar");
        estadoVuelo estadoAnterior = vuelo.getEstado();
        prepararModificacion(vuelo, vueloDTO);
        Vuelo guardado = vueloRepository.save(vuelo);

        // Pasarlo a CANCELADO desde "Modificar" tiene el mismo efecto que darlo de baja
        if (estadoAnterior != estadoVuelo.CANCELADO && guardado.getEstado() == estadoVuelo.CANCELADO) {
            cancelacionVueloService.cancelarReservasDelVuelo(guardado);
        }
        return guardado;
    }

    /**
     * Corre las mismas validaciones que el alta (vueloId == null) o la modificación,
     * pero sin guardar nada. El panel de admin lo usa para avisar los errores en vivo
     * (por ejemplo, que el avión ya tiene un vuelo en ese horario).
     */
    public ValidacionVueloDTO validarVuelo(Integer vueloId, VueloDTO vueloDTO) {
        try {
            // Se trabaja sobre un borrador para no modificar la entidad gestionada por JPA
            Vuelo borrador = new Vuelo();
            if (vueloId == null) {
                prepararAlta(borrador, vueloDTO);
            } else {
                validarNoFinalizado(buscarVuelo(vueloId), "modificar");
                borrador.setId(vueloId);
                prepararModificacion(borrador, vueloDTO);
            }
            return ValidacionVueloDTO.ok();
        } catch (VueloInvalidoException | AvionInvalidoException | AeropuertoInvalidoException e) {
            return ValidacionVueloDTO.error(e.getMessage());
        }
    }

    private void prepararAlta(Vuelo vuelo, VueloDTO vueloDTO) {
        validarCamposObligatorios(vueloDTO);

        estadoVuelo estado = vueloDTO.getEstado() != null ? vueloDTO.getEstado() : estadoVuelo.PROGRAMADO;
        if (estado == estadoVuelo.CANCELADO) {
            throw new VueloInvalidoException("No se puede dar de alta un vuelo en estado CANCELADO.");
        }

        LocalDateTime salida = LocalDateTime.of(vueloDTO.getFechaSalida(), vueloDTO.getHoraSalida());
        if (!salida.isAfter(LocalDateTime.now())) {
            throw new VueloInvalidoException("La fecha y hora de salida debe ser posterior al momento actual.");
        }

        cargarDatosVuelo(vuelo, vueloDTO);
        vuelo.setEstado(estado);

        validarDatosVuelo(vuelo);
        validarAvionDisponible(vuelo.getAvion().getId());
        validarDisponibilidadAvion(
                vuelo.getAvion().getId(),
                vuelo.getFechaSalida(),
                vuelo.getFechaLlegada(),
                vuelo.getHoraSalida(),
                vuelo.getHoraLlegada(),
                -1);
    }

    private void prepararModificacion(Vuelo vuelo, VueloDTO vueloDTO) {
        validarCamposObligatorios(vueloDTO);
        if (vueloDTO.getEstado() == null) {
            throw new VueloInvalidoException("El estado del vuelo es obligatorio.");
        }

        cargarDatosVuelo(vuelo, vueloDTO);
        vuelo.setEstado(vueloDTO.getEstado());

        validarDatosVuelo(vuelo);

        // Un vuelo cancelado no ocupa el avión, así que solo se valida si sigue operativo
        if (vuelo.getEstado() != estadoVuelo.CANCELADO) {
            validarAvionDisponible(vuelo.getAvion().getId());
            validarDisponibilidadAvion(
                    vuelo.getAvion().getId(),
                    vuelo.getFechaSalida(),
                    vuelo.getFechaLlegada(),
                    vuelo.getHoraSalida(),
                    vuelo.getHoraLlegada(),
                    vuelo.getId());
        }
    }

    private Vuelo buscarVuelo(Integer id) {
        return vueloRepository.findById(id)
                .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado. ID: " + id));
    }

    /** Un vuelo cuya fecha y hora de llegada ya pasó se considera finalizado. */
    public static boolean estaFinalizado(Vuelo vuelo) {
        if (vuelo.getFechaLlegada() == null || vuelo.getHoraLlegada() == null) {
            return false;
        }
        return LocalDateTime.of(vuelo.getFechaLlegada(), vuelo.getHoraLlegada()).isBefore(LocalDateTime.now());
    }

    private void validarNoFinalizado(Vuelo vuelo, String accion) {
        if (estaFinalizado(vuelo)) {
            throw new VueloInvalidoException(
                    "No se puede " + accion + " el vuelo #" + vuelo.getId() + " porque ya finalizó.");
        }
    }

    private void validarCamposObligatorios(VueloDTO dto) {
        if (dto == null) {
            throw new VueloInvalidoException("Los datos del vuelo son obligatorios.");
        }
        if (dto.getAeropuertoOrigenId() == null || dto.getAeropuertoDestinoId() == null) {
            throw new VueloInvalidoException("El aeropuerto de origen y destino son obligatorios.");
        }
        if (dto.getAvionId() == null) {
            throw new VueloInvalidoException("El avión es obligatorio.");
        }
        if (dto.getFechaSalida() == null || dto.getFechaLlegada() == null) {
            throw new VueloInvalidoException("La fecha de salida y llegada son obligatorias.");
        }
        if (dto.getHoraSalida() == null || dto.getHoraLlegada() == null) {
            throw new VueloInvalidoException("La hora de salida y llegada son obligatorias.");
        }
        if (dto.getEscala() == null) {
            throw new VueloInvalidoException("Indicá si el vuelo es directo o con escala.");
        }
    }

    private void cargarDatosVuelo(Vuelo vuelo, VueloDTO vueloDTO) {
        Aeropuerto origen = aeropuertoRepository.findById(vueloDTO.getAeropuertoOrigenId())
                .orElseThrow(() -> new AeropuertoInvalidoException(
                        "Aeropuerto origen no encontrado. ID: " + vueloDTO.getAeropuertoOrigenId()));

        Aeropuerto destino = aeropuertoRepository.findById(vueloDTO.getAeropuertoDestinoId())
                .orElseThrow(() -> new AeropuertoInvalidoException(
                        "Aeropuerto destino no encontrado. ID: " + vueloDTO.getAeropuertoDestinoId()));

        Avion avion = avionRepository.findById(vueloDTO.getAvionId())
                .orElseThrow(() -> new AvionInvalidoException(
                        "Avión no encontrado. ID: " + vueloDTO.getAvionId()));

        vuelo.setAeropuertoOrigen(origen);
        vuelo.setAeropuertoDestino(destino);
        vuelo.setAvion(avion);
        vuelo.setFechaSalida(vueloDTO.getFechaSalida());
        vuelo.setFechaLlegada(vueloDTO.getFechaLlegada());
        vuelo.setHoraSalida(vueloDTO.getHoraSalida());
        vuelo.setHoraLlegada(vueloDTO.getHoraLlegada());
        vuelo.setPrecioVuelo(vueloDTO.getPrecioVuelo());
        vuelo.setEscala(vueloDTO.getEscala());
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

        // Ej.: EZE y AEP son aeropuertos distintos pero ambos están en Buenos Aires
        String ciudadOrigen = normalizarCiudad(vuelo.getAeropuertoOrigen().getCiudad());
        if (!ciudadOrigen.isEmpty()
                && ciudadOrigen.equals(normalizarCiudad(vuelo.getAeropuertoDestino().getCiudad()))) {
            throw new VueloInvalidoException(
                    "El origen y el destino no pueden estar en la misma ciudad ("
                            + vuelo.getAeropuertoOrigen().getCiudad() + ").");
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
                    "La duración total de un vuelo con escala no puede exceder las 36 horas.");
        }
    }

    private static String normalizarCiudad(String ciudad) {
        if (ciudad == null) return "";
        return Normalizer.normalize(ciudad, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
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
    @Transactional
    public Vuelo darDeBajaVuelo(Integer id) {
        Vuelo vuelo = buscarVuelo(id);

        if (vuelo.getEstado() == estadoVuelo.CANCELADO) {
            throw new VueloInvalidoException("El vuelo ya se encuentra dado de baja (CANCELADO).");
        }
        validarNoFinalizado(vuelo, "dar de baja");

        // Baja lógica: el vuelo queda registrado (reservas, pasajes e historial lo referencian)
        vuelo.setEstado(estadoVuelo.CANCELADO);
        Vuelo guardado = vueloRepository.save(vuelo);
        // Las reservas del vuelo se cancelan con reembolso total y se avisa a los clientes
        cancelacionVueloService.cancelarReservasDelVuelo(guardado);
        return guardado;
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