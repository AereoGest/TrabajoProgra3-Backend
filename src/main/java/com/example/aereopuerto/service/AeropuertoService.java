package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AeropuertoInvalidoException;
import com.example.aereopuerto.Specifications.AeropuertoSpecification;
import com.example.aereopuerto.dto.AeropuertoDTO;
import com.example.aereopuerto.model.Aeropuerto;
import com.example.aereopuerto.repository.AeropuertoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class AeropuertoService {

    private static final Pattern NOMBRE_VALIDO =
            Pattern.compile("^(?=(?:.*[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ]){4,})[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+$");
    private static final Pattern SOLO_LETRAS =
            Pattern.compile("^[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+$");
    private static final Pattern CODIGO_IATA =
            Pattern.compile("^[A-Za-z]{3}$");

    @Autowired
    private final AeropuertoRepository aeropuertoRepository;

    @Cacheable(value = "aeropuertos", key = "#id")
    public Aeropuerto obtenerAeropuertoPorId(Integer id) {
        return aeropuertoRepository.findById(id)
                    .orElseThrow(() -> new AeropuertoInvalidoException("Aeropuerto no encontrado. ID: " + id));
    }

    @Cacheable(value = "aeropuertos", key = "'todos'")
    public List<Aeropuerto> obtenerTodosLosAeropuertos() {
        return aeropuertoRepository.findAll();
    }

    @CachePut(value = "aeropuertos", key = "#result.id")
    @CacheEvict(value = "aeropuertos", key = "'todos'")
    public Aeropuerto crearAeropuerto(AeropuertoDTO dto) {
        validarDatos(dto);
        validarCodigoIataUnico(dto.getCodigoIata(), null);

        Aeropuerto aeropuerto = Aeropuerto.builder()
                .nombre(dto.getNombre().trim())
                .codigoIata(dto.getCodigoIata().trim().toUpperCase())
                .ciudad(dto.getCiudad().trim())
                .pais(dto.getPais().trim())
                .build();

        return aeropuertoRepository.save(aeropuerto);
    }

    @CachePut(value = "aeropuertos", key = "#result.id")
    @CacheEvict(value = "aeropuertos", key = "'todos'")
    public Aeropuerto EditarAeropuerto(Integer id, AeropuertoDTO dto) {
        Aeropuerto a = aeropuertoRepository.findById(id)
                .orElseThrow(() -> new AeropuertoInvalidoException("Aeropuerto no encontrado. ID: " + id));

        validarDatos(dto);
        validarCodigoIataUnico(dto.getCodigoIata(), id);

        a.setNombre(dto.getNombre().trim());
        a.setCodigoIata(dto.getCodigoIata().trim().toUpperCase());
        a.setCiudad(dto.getCiudad().trim());
        a.setPais(dto.getPais().trim());

        return aeropuertoRepository.save(a);
    }

    private void validarDatos(AeropuertoDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("Los datos del aeropuerto son obligatorios.");
        }
        String nombre = dto.getNombre() != null ? dto.getNombre().trim() : "";
        if (nombre.isEmpty() || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            throw new IllegalArgumentException("El nombre solo puede contener letras y espacios, con al menos 4 letras.");
        }
        String iata = dto.getCodigoIata() != null ? dto.getCodigoIata().trim() : "";
        if (!CODIGO_IATA.matcher(iata).matches()) {
            throw new IllegalArgumentException("El código IATA debe tener exactamente 3 letras.");
        }
        String ciudad = dto.getCiudad() != null ? dto.getCiudad().trim() : "";
        if (ciudad.isEmpty() || !SOLO_LETRAS.matcher(ciudad).matches()) {
            throw new IllegalArgumentException("La ciudad solo puede contener letras y espacios.");
        }
        String pais = dto.getPais() != null ? dto.getPais().trim() : "";
        if (pais.isEmpty() || !SOLO_LETRAS.matcher(pais).matches()) {
            throw new IllegalArgumentException("El país solo puede contener letras y espacios.");
        }
    }

    // Valida que el código IATA no esté en uso por otro aeropuerto
    private void validarCodigoIataUnico(String codigoIata, Integer excludeId) {
        aeropuertoRepository.findByCodigoIata(codigoIata.toUpperCase())
                .ifPresent(existing -> {
                    if (!existing.getId().equals(excludeId)) {
                        throw new AeropuertoInvalidoException(
                                "Ya existe un aeropuerto con el código IATA: " + codigoIata.toUpperCase()
                        );
                    }
                });
    }

    @Caching(evict = {
            @CacheEvict(value = "aeropuertos", key = "#id"),
            @CacheEvict(value = "aeropuertos", key = "'todos'")
    })
    public void eliminarAeropuerto(Integer id) {
        aeropuertoRepository.deleteById(id);
    }

    public List<Aeropuerto> buscarAeropuertosConFiltros(
            String nombre,
            String codigoIata,
            String ciudad,
            String pais) {

        Specification<Aeropuerto> spec = Specification
                .where(AeropuertoSpecification.porNombre(nombre))
                .and(AeropuertoSpecification.porCodigoIata(codigoIata))
                .and(AeropuertoSpecification.porCiudad(ciudad))
                .and(AeropuertoSpecification.porPais(pais));

        return aeropuertoRepository.findAll(spec);
    }

    @CacheEvict(value = "aeropuertos", key = "'todos'")
    public void invalidarListaDeAeropuertos() {
    }
}
