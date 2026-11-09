package com.example.aereopuerto.service;


import com.example.aereopuerto.Exceptions.AsignacionInvalidaException;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.model.Asignacion;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.repository.AsignacionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AsignacionService {

    @Autowired
    private AsignacionRepository asignacionRepository;
    @Autowired
    private UsuarioAutenticadoService usuarioAutenticadoService;

    @Cacheable(value = "asignacion", key = "#id")
    public Asignacion obtenerAsignacionPorId(Integer id) {
        return asignacionRepository.findById(id)
                .orElseThrow(() -> new AsignacionInvalidaException("Asignacion no encontrada. ID: " + id));
    }

    @Cacheable(value = "asignacion", key = "'todas'")
    public List<Asignacion> obtenerTodosLasAsignaciones() {
        return asignacionRepository.findAll();
    }

    @CachePut(value = "asignacion", key = "#result.id")
    @CacheEvict(value = "asignacion", key = "'todas'")
    public Asignacion crearAsignacion(Asignacion asignacion) {

        return asignacionRepository.save(asignacion);
    }

    @CachePut(value = "asignacion", key = "#result.id")
    @CacheEvict(value = "asignacion", key = "'todas'")
    public Asignacion EditarAeropuerto(Integer id, Asignacion asignacion) {
        Asignacion a= asignacionRepository.findById(id).orElseThrow(() -> new AsignacionInvalidaException("Asignacion no encontrada. ID: " + id));
        a.setEmpleado(asignacion.getEmpleado());
        a.setVuelo(asignacion.getVuelo());
        a.setRolEmpleado(asignacion.getRolEmpleado());

        return asignacionRepository.save(a);
    }

    @Caching(evict = {
            @CacheEvict(value = "asignacion", key = "#id"),
            @CacheEvict(value = "asignacion", key = "'todas'")
    })
    public void eliminarAsignacion(Integer id) {
        asignacionRepository.deleteById(id);
    }

    @CacheEvict(value = "asignacion", key = "'todas'")
    public void invalidarListaDeAsignaciones() {
    }


    /**
     * Devuelve las asignaciones del empleado logueado (sale del token).
     * No se cachea porque depende del usuario.
     */
    public List<Asignacion> obtenerMisAsignaciones(String email) {
        Persona empleado = usuarioAutenticadoService.getPersonaPorEmail(email);
        return asignacionRepository.findByEmpleadoIdOrderByVueloFechaSalidaAscVueloHoraSalidaAsc(empleado.getId());
    }
}
