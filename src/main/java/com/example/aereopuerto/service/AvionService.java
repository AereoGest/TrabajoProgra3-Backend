package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.Specifications.AvionSpecification;
import com.example.aereopuerto.dto.AvionDTO;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.estadoAvion;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.AvionRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import com.example.aereopuerto.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AvionService {

    /** Estados de vuelo que todavía "ocupan" al avión (si el vuelo no finalizó). */
    private static final List<estadoVuelo> ESTADOS_PENDIENTES = List.of(
            estadoVuelo.PROGRAMADO, estadoVuelo.REPROGRAMADO, estadoVuelo.BOARDING, estadoVuelo.ACTIVO);

    /** Límite razonable: el avión comercial más grande (A380) admite ~850 pasajeros. */
    static final int MAX_CAPACIDAD_PASAJEROS = 900;
    static final int MAX_LARGO_MODELO = 100;
    static final String PATRON_IDENTIFICADOR = "^LV-(A\\d+|[A-Z]{3})$";

    private final AvionRepository avionRepository;
    private final VueloRepository vueloRepository;
    private final PasajeRepository pasajeRepository;
    private final AsientoService asientoService;

    @Cacheable(value = "aviones", key = "#id")
    public Avion obtenerAvionPorId(Integer id) {
        return buscarAvion(id);
    }

    @Cacheable(value = "aviones", key = "'todos'")
    public List<Avion> obtenerTodosLosAviones() {
        return avionRepository.findAll();
    }

    // ───────────────────────── Alta ─────────────────────────

    @Caching(put = { @CachePut(value = "aviones", key = "#result.id") }, evict = {
            @CacheEvict(value = "aviones", key = "'todos'")
    })
    @Transactional
    public Avion crearAvion(AvionDTO dto) {
        validarCamposObligatorios(dto);

        estadoAvion estado = dto.getEstado() != null ? dto.getEstado() : estadoAvion.DISPONIBLE;
        if (estado == estadoAvion.BAJA) {
            throw new AvionInvalidoException("No se puede dar de alta un avión en estado BAJA.");
        }

        Avion avion = new Avion();
        cargarDatos(avion, dto);
        avion.setEstado(estado);

        validarAvion(avion);
        validarIdentificadorUnico(avion.getIdentificador(), null);

        Avion guardado = avionRepository.save(avion);
        // Se generan los asientos según la capacidad (primera, ejecutiva y económica)
        asientoService.sincronizarAsientos(guardado);
        return guardado;
    }

    // ───────────────────────── Modificación ─────────────────────────

    // Los vuelos guardan una copia del avión en la caché, por eso también se limpia "vuelos"
    @Caching(put = { @CachePut(value = "aviones", key = "#result.id") }, evict = {
            @CacheEvict(value = "aviones", key = "'todos'"),
            @CacheEvict(value = "vuelos", allEntries = true)
    })
    @Transactional
    public Avion actualizarAvion(Integer id, AvionDTO dto) {
        Avion avion = buscarAvion(id);

        validarCamposObligatorios(dto);
        if (dto.getEstado() == null) {
            throw new AvionInvalidoException("El estado del avión es obligatorio.");
        }

        // Se valida sobre un borrador para no tocar la entidad si algo falla
        Avion borrador = new Avion();
        borrador.setId(id);
        cargarDatos(borrador, dto);
        borrador.setEstado(dto.getEstado());
        validarAvion(borrador);
        validarIdentificadorUnico(borrador.getIdentificador(), id);

        List<Vuelo> pendientes = vuelosPendientes(id);

        boolean saleDeServicio = dto.getEstado() == estadoAvion.MANTENIMIENTO || dto.getEstado() == estadoAvion.BAJA;
        if (saleDeServicio && avion.getEstado() != dto.getEstado() && !pendientes.isEmpty()) {
            String estadoNombre = dto.getEstado() == estadoAvion.MANTENIMIENTO ? "mantenimiento" : "baja";
            throw new AvionInvalidoException(
                    "No se puede pasar el avión " + avion.getIdentificador() + " a " + estadoNombre
                            + " porque tiene " + describirVuelos(pendientes)
                            + ". Reasigná o cancelá esos vuelos primero.");
        }

        // No se puede achicar la capacidad por debajo de los pasajes ya vendidos
        if (dto.getCapacidadPasajeros() < avion.getCapacidadPasajeros()) {
            for (Vuelo vuelo : pendientes) {
                long vendidos = pasajeRepository.countByVueloId(vuelo.getId());
                if (vendidos > dto.getCapacidadPasajeros()) {
                    throw new AvionInvalidoException(
                            "No se puede reducir la capacidad a " + dto.getCapacidadPasajeros()
                                    + " pasajeros: el vuelo #" + vuelo.getId() + " ya tiene "
                                    + vendidos + " pasajes vendidos.");
                }
            }
        }

        // Al achicar, tiene que haber suficientes asientos libres para quitar
        asientoService.validarCambioCapacidad(id, dto.getCapacidadPasajeros());

        cargarDatos(avion, dto);
        avion.setEstado(dto.getEstado());
        Avion guardado = avionRepository.save(avion);
        // Agrega o quita asientos para que coincidan con la capacidad
        // (también completa los que le falten a aviones cargados antes de esta funcionalidad)
        asientoService.sincronizarAsientos(guardado);
        return guardado;
    }

    // ───────────────────────── Baja lógica ─────────────────────────

    @Caching(evict = {
            @CacheEvict(value = "aviones", key = "#id"),
            @CacheEvict(value = "aviones", key = "'todos'"),
            @CacheEvict(value = "vuelos", allEntries = true)
    })
    public Avion darDeBajaAvion(Integer id) {
        Avion avion = buscarAvion(id);

        if (avion.getEstado() == estadoAvion.BAJA) {
            throw new AvionInvalidoException("El avión " + avion.getIdentificador() + " ya está dado de baja.");
        }

        List<Vuelo> pendientes = vuelosPendientes(id);
        if (!pendientes.isEmpty()) {
            throw new AvionInvalidoException(
                    "No se puede dar de baja el avión " + avion.getIdentificador() + " porque tiene "
                            + describirVuelos(pendientes) + ". Reasigná o cancelá esos vuelos primero.");
        }

        // Baja lógica: el avión se conserva porque vuelos, asientos e historial lo referencian
        avion.setEstado(estadoAvion.BAJA);
        return avionRepository.save(avion);
    }

    // ───────────────────────── Validaciones ─────────────────────────

    private Avion buscarAvion(Integer id) {
        return avionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Avión no encontrado. ID: " + id));
    }

    private void validarCamposObligatorios(AvionDTO dto) {
        if (dto == null) {
            throw new AvionInvalidoException("Los datos del avión son obligatorios.");
        }
        if (dto.getIdentificador() == null || dto.getIdentificador().isBlank()) {
            throw new AvionInvalidoException("El identificador (matrícula) es obligatorio.");
        }
        if (dto.getModelo() == null || dto.getModelo().isBlank()) {
            throw new AvionInvalidoException("El modelo es obligatorio.");
        }
        if (dto.getCapacidadPasajeros() == null) {
            throw new AvionInvalidoException("La capacidad de pasajeros es obligatoria.");
        }
        if (dto.getCapacidadBodega() == null) {
            throw new AvionInvalidoException("La capacidad de bodega es obligatoria.");
        }
    }

    private void cargarDatos(Avion avion, AvionDTO dto) {
        avion.setIdentificador(dto.getIdentificador().trim().toUpperCase());
        avion.setModelo(dto.getModelo().trim().replaceAll("\\s+", " "));
        avion.setCapacidadPasajeros(dto.getCapacidadPasajeros());
        avion.setCapacidadBodega(dto.getCapacidadBodega());
    }

    private void validarAvion(Avion avion) {
        if (avion.getCapacidadPasajeros() == null || avion.getCapacidadPasajeros() <= 0) {
            throw new AvionInvalidoException("La capacidad de pasajeros debe ser mayor a 0.");
        }
        if (avion.getCapacidadPasajeros() > MAX_CAPACIDAD_PASAJEROS) {
            throw new AvionInvalidoException(
                    "La capacidad de pasajeros no puede superar " + MAX_CAPACIDAD_PASAJEROS + ".");
        }

        if (avion.getCapacidadBodega() == null || avion.getCapacidadBodega() <= 0) {
            throw new AvionInvalidoException("La capacidad de bodega debe ser mayor a 0.");
        }

        if (avion.getModelo() == null || avion.getModelo().isBlank()) {
            throw new AvionInvalidoException("El modelo es obligatorio.");
        }
        if (avion.getModelo().length() > MAX_LARGO_MODELO) {
            throw new AvionInvalidoException(
                    "El modelo no puede superar los " + MAX_LARGO_MODELO + " caracteres.");
        }

        // Formato de matrícula: argentina (LV- + 3 letras, ej. LV-KFX) o interna (LV-A + números, ej. LV-A25)
        String identificador = avion.getIdentificador();
        if (identificador == null || !identificador.matches(PATRON_IDENTIFICADOR)) {
            throw new AvionInvalidoException(
                    "El identificador debe ser LV- seguido de 3 letras (ej. LV-KFX) o LV-A seguido de números (ej. LV-A25).");
        }
    }

    private void validarIdentificadorUnico(String identificador, Integer idActual) {
        boolean existe = idActual == null
                ? avionRepository.existsByIdentificadorIgnoreCase(identificador)
                : avionRepository.existsByIdentificadorIgnoreCaseAndIdNot(identificador, idActual);
        if (existe) {
            throw new AvionInvalidoException("Ya existe un avión con el identificador " + identificador + ".");
        }
    }

    /** Vuelos del avión que todavía no se hicieron (programados, reprogramados, embarcando o en vuelo). */
    private List<Vuelo> vuelosPendientes(Integer avionId) {
        return vueloRepository.findByAvionIdAndEstadoIn(avionId, ESTADOS_PENDIENTES).stream()
                .filter(v -> !VueloService.estaFinalizado(v))
                .toList();
    }

    private String describirVuelos(List<Vuelo> vuelos) {
        String ids = vuelos.stream().limit(5).map(v -> "#" + v.getId()).collect(Collectors.joining(", "));
        String extra = vuelos.size() > 5 ? " y otros" : "";
        return vuelos.size() + " vuelo(s) pendiente(s) (" + ids + extra + ")";
    }

    // ───────────────────────── Búsqueda ─────────────────────────

    public List<Avion> buscarAvionesConFiltros(
            String identificador,
            String modelo,
            estadoAvion estado,
            Integer capacidadPasajerosMin,
            Integer capacidadPasajerosMax,
            Double capacidadBodegaMin,
            Double capacidadBodegaMax) {

        Specification<Avion> spec = Specification
                .where(AvionSpecification.porIdentificador(identificador))
                .and(AvionSpecification.porModelo(modelo))
                .and(AvionSpecification.porEstado(estado))
                .and(AvionSpecification.capacidadPasajerosMinimaDesde(capacidadPasajerosMin))
                .and(AvionSpecification.capacidadPasajerosMaximaHasta(capacidadPasajerosMax))
                .and(AvionSpecification.capacidadBodegaMinimaDesde(capacidadBodegaMin))
                .and(AvionSpecification.capacidadBodegaMaximaHasta(capacidadBodegaMax));

        return avionRepository.findAll(spec);
    }

    @CacheEvict(value = "aviones", key = "'todos'")
    public void invalidarListaDeAviones() {
    }
}
