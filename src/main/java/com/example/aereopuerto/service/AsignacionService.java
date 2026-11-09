package com.example.aereopuerto.service;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.dto.AsignacionDTO;
import com.example.aereopuerto.model.Asignacion;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.RolEmpleado;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.AsignacionRepository;
import com.example.aereopuerto.repository.PersonaRepository;
import com.example.aereopuerto.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import com.example.aereopuerto.Specifications.AsignacionSpecification;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.time.LocalDate;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    private final AsignacionRepository asignacionRepository;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final VueloRepository vueloRepository;
    private final PersonaRepository personaRepository;
    private final UserRepository userRepository;

    @Cacheable(value = "asignacion", key = "#id")
    public Asignacion obtenerAsignacionPorId(Integer id) {
        return asignacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada. ID: " + id));
    }

    @Cacheable(value = "asignacion", key = "'todas'")
    public List<Asignacion> obtenerTodosLasAsignaciones() {
        return asignacionRepository.findAll();
    }

    @Transactional
    @CachePut(value = "asignacion", key = "#result.id")
    @CacheEvict(value = "asignacion", key = "'todas'")
    public Asignacion crearAsignacion(AsignacionDTO dto) {
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la asignación son obligatorios");
        }
        if (dto.getVueloId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de vuelo es obligatorio");
        }
        if (dto.getEmpleadoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de empleado es obligatorio");
        }
        if (dto.getRolEmpleado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol del empleado es obligatorio");
        }

        Vuelo vuelo = validarVuelo(dto.getVueloId());
        Persona empleado = validarEmpleado(dto.getEmpleadoId());

        // Prevenir duplicados (mismo empleado en el mismo vuelo)
        if (asignacionRepository.existsByVueloIdAndEmpleadoId(vuelo.getId(), empleado.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado ya se encuentra asignado al vuelo #" + vuelo.getId());
        }

        // Prevenir conflicto horario con otros vuelos asignados al empleado
        validarConflictoHorario(empleado.getId(), vuelo, null);

        Asignacion nueva = Asignacion.builder()
                .vuelo(vuelo)
                .empleado(empleado)
                .rolEmpleado(dto.getRolEmpleado())
                .build();

        return asignacionRepository.save(nueva);
    }

    /**
     * Sobrecarga para mantener retrocompatibilidad si se invoca con la entidad.
     */
    @Transactional
    public Asignacion crearAsignacion(Asignacion asignacion) {
        if (asignacion == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La asignación no puede ser nula");
        }
        Integer vueloId = asignacion.getVuelo() != null ? asignacion.getVuelo().getId() : null;
        Integer empleadoId = asignacion.getEmpleado() != null ? asignacion.getEmpleado().getId() : null;
        RolEmpleado rol = asignacion.getRolEmpleado();

        return crearAsignacion(new AsignacionDTO(vueloId, empleadoId, rol));
    }

    @Transactional
    @CachePut(value = "asignacion", key = "#result.id")
    @CacheEvict(value = "asignacion", key = "'todas'")
    public Asignacion actualizarAsignacion(Integer id, AsignacionDTO dto) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de asignación es obligatorio");
        }
        if (dto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los datos de la asignación son obligatorios");
        }
        if (dto.getVueloId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de vuelo es obligatorio");
        }
        if (dto.getEmpleadoId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El ID de empleado es obligatorio");
        }
        if (dto.getRolEmpleado() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El rol del empleado es obligatorio");
        }

        Asignacion existente = asignacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada. ID: " + id));

        Vuelo vuelo = validarVuelo(dto.getVueloId());
        Persona empleado = validarEmpleado(dto.getEmpleadoId());

        // Prevenir duplicados (mismo empleado en el mismo vuelo, excluyendo esta asignación)
        if (asignacionRepository.existsByVueloIdAndEmpleadoIdAndIdNot(vuelo.getId(), empleado.getId(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "El empleado ya se encuentra asignado al vuelo #" + vuelo.getId());
        }

        // Prevenir conflicto horario con otros vuelos asignados al empleado
        validarConflictoHorario(empleado.getId(), vuelo, id);

        existente.setVuelo(vuelo);
        existente.setEmpleado(empleado);
        existente.setRolEmpleado(dto.getRolEmpleado());

        return asignacionRepository.save(existente);
    }

    /**
     * Alias para retrocompatibilidad con llamadas legacy.
     */
    @Transactional
    public Asignacion EditarAeropuerto(Integer id, Asignacion asignacion) {
        Integer vueloId = asignacion != null && asignacion.getVuelo() != null ? asignacion.getVuelo().getId() : null;
        Integer empleadoId = asignacion != null && asignacion.getEmpleado() != null ? asignacion.getEmpleado().getId() : null;
        RolEmpleado rol = asignacion != null ? asignacion.getRolEmpleado() : null;
        return actualizarAsignacion(id, new AsignacionDTO(vueloId, empleadoId, rol));
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "asignacion", key = "#id"),
            @CacheEvict(value = "asignacion", key = "'todas'")
    })
    public void eliminarAsignacion(Integer id) {
        Asignacion asignacion = asignacionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Asignación no encontrada. ID: " + id));
        asignacionRepository.delete(asignacion);
    }

    @CacheEvict(value = "asignacion", key = "'todas'")
    public void invalidarListaDeAsignaciones() {
    }

    /**
     * Devuelve las asignaciones del empleado logueado (sale del token).
     */
    public List<Asignacion> obtenerMisAsignaciones(String email) {
        Persona empleado = usuarioAutenticadoService.getPersonaPorEmail(email);
        return asignacionRepository.findByEmpleadoIdOrderByVueloFechaSalidaAscVueloHoraSalidaAsc(empleado.getId());
    }

    /**
     * Devuelve la lista de personas que tienen rol de empleado o administrador para asignarlas en vuelos.
     */
    /**
     * Filtra asignaciones según múltiples criterios combinables.
     */
    public List<Asignacion> buscarAsignacionesConFiltros(
            Integer empleadoId,
            Integer vueloId,
            RolEmpleado rolEmpleado,
            estadoVuelo estadoVuelo,
            LocalDate fechaDesde,
            LocalDate fechaHasta,
            String busqueda) {

        Specification<Asignacion> spec = Specification
                .where(AsignacionSpecification.porEmpleadoId(empleadoId))
                .and(AsignacionSpecification.porVueloId(vueloId))
                .and(AsignacionSpecification.porRolEmpleado(rolEmpleado))
                .and(AsignacionSpecification.porEstadoVuelo(estadoVuelo))
                .and(AsignacionSpecification.porFechaSalidaDesde(fechaDesde))
                .and(AsignacionSpecification.porFechaSalidaHasta(fechaHasta))
                .and(AsignacionSpecification.porTexto(busqueda));

        Sort sort = Sort.by(Sort.Direction.ASC, "vuelo.fechaSalida", "vuelo.horaSalida");
        return asignacionRepository.findAll(spec, sort);
    }

    public List<Persona> obtenerEmpleadosDisponibles() {
        List<User> usuarios = userRepository.findByRoleIn(List.of(Role.ROLE_EMPLEADO, Role.ROLE_ADMIN));
        if (usuarios != null && !usuarios.isEmpty()) {
            return usuarios.stream()
                    .map(User::getPersona)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return personaRepository.findAll();
    }

    // ================= Validaciones privadas =================

    private Vuelo validarVuelo(Integer vueloId) {
        Vuelo vuelo = vueloRepository.findById(vueloId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vuelo no encontrado con ID: " + vueloId));

        if (vuelo.getEstado() == estadoVuelo.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede asignar personal a un vuelo cancelado");
        }

        if (vuelo.getFechaLlegada() != null && vuelo.getHoraLlegada() != null) {
            LocalDateTime llegada = LocalDateTime.of(vuelo.getFechaLlegada(), vuelo.getHoraLlegada());
            if (llegada.isBefore(LocalDateTime.now())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede asignar personal a un vuelo finalizado");
            }
        }

        return vuelo;
    }

    private Persona validarEmpleado(Integer empleadoId) {
        Persona empleado = personaRepository.findById(empleadoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Empleado no encontrado con ID: " + empleadoId));

        Optional<User> userOpt = userRepository.findByPersonaId(empleado.getId());
        if (userOpt.isPresent()) {
            Role rol = userOpt.get().getRole();
            if (rol != Role.ROLE_EMPLEADO && rol != Role.ROLE_ADMIN) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La persona seleccionada (" + empleado.getNombre() + " " + empleado.getApellido() +
                                ") no posee rol de empleado ni administrador");
            }
        }

        return empleado;
    }

    private void validarConflictoHorario(Integer empleadoId, Vuelo vueloNuevo, Integer asignacionIdExcluir) {
        if (vueloNuevo.getFechaSalida() == null || vueloNuevo.getHoraSalida() == null ||
                vueloNuevo.getFechaLlegada() == null || vueloNuevo.getHoraLlegada() == null) {
            return;
        }

        LocalDateTime nuevoInicio = LocalDateTime.of(vueloNuevo.getFechaSalida(), vueloNuevo.getHoraSalida());
        LocalDateTime nuevoFin = LocalDateTime.of(vueloNuevo.getFechaLlegada(), vueloNuevo.getHoraLlegada());

        List<Asignacion> asignaciones = asignacionRepository.findByEmpleadoId(empleadoId);
        for (Asignacion a : asignaciones) {
            if (asignacionIdExcluir != null && a.getId().equals(asignacionIdExcluir)) {
                continue;
            }
            Vuelo otro = a.getVuelo();
            if (otro == null || otro.getEstado() == estadoVuelo.CANCELADO || otro.getId().equals(vueloNuevo.getId())) {
                continue;
            }
            if (otro.getFechaSalida() == null || otro.getHoraSalida() == null ||
                    otro.getFechaLlegada() == null || otro.getHoraLlegada() == null) {
                continue;
            }

            LocalDateTime otroInicio = LocalDateTime.of(otro.getFechaSalida(), otro.getHoraSalida());
            LocalDateTime otroFin = LocalDateTime.of(otro.getFechaLlegada(), otro.getHoraLlegada());

            boolean solapa = otroInicio.isBefore(nuevoFin) && otroFin.isAfter(nuevoInicio);
            if (solapa) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "El empleado tiene conflicto horario con el vuelo #" + otro.getId() +
                                " (" + otro.getFechaSalida() + " " + otro.getHoraSalida() + " a " +
                                otro.getFechaLlegada() + " " + otro.getHoraLlegada() + ")");
            }
        }
    }
}
