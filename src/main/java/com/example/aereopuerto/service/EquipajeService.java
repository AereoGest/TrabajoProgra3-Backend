package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.EquipajeInvalidoException;
import com.example.aereopuerto.model.Equipaje;
import com.example.aereopuerto.repository.EquipajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EquipajeService {

    @Autowired
    private final EquipajeRepository equipajeRepository;

    @Cacheable(value = "equipajes", key = "#id")
    public Equipaje obtenerEquipajePorId(Integer id) {
        return equipajeRepository.findById(id).orElseThrow(() -> new EquipajeInvalidoException("Equipaje no encontrado. ID: " + id));
    }

    @Cacheable(value = "equipajes", key = "'todos'")
    public List<Equipaje> obtenerTodosLosEquipajes() {
        return equipajeRepository.findAll();
    }

    @CachePut(value = "equipajes", key = "#result.id")
    @CacheEvict(value = "equipajes", key = "'todos'")
    public Equipaje crearEquipaje(Equipaje equipaje) {
        // hay un solo registro por tipo: para cambiar peso o precio se edita el existente
        if (equipajeRepository.existsByTipo(equipaje.getTipo())) {
            throw new IllegalArgumentException("Ya existe un equipaje de tipo " + equipaje.getTipo() + ". Editá el existente.");
        }
        return equipajeRepository.save(equipaje);
    }

    @CachePut(value = "equipajes", key = "#result.id")
    @CacheEvict(value = "equipajes", key = "'todos'")
    public Equipaje actualizarEquipaje(Integer id, Equipaje equipaje) {

        Equipaje e = equipajeRepository.findById(id)
                .orElseThrow(() -> new EquipajeInvalidoException("Equipaje no encontrado. ID: " + id));

        if (equipajeRepository.existsByTipoAndIdNot(equipaje.getTipo(), id)) {
            throw new IllegalArgumentException("Ya existe un equipaje de tipo " + equipaje.getTipo() + ".");
        }

        e.setPeso(equipaje.getPeso());
        e.setTipo(equipaje.getTipo());
        e.setPrecio(equipaje.getPrecio());

        return equipajeRepository.save(e);
    }

    @Caching(evict = {
            @CacheEvict(value = "equipajes", key = "#id"),
            @CacheEvict(value = "equipajes", key = "'todos'")
    })
    public void eliminarEquipaje(Integer id) {
        equipajeRepository.deleteById(id);
    }

    @CacheEvict(value = "equipajes", key = "'todos'")
    public void invalidarListaDeEquipajes() {
    }
}
