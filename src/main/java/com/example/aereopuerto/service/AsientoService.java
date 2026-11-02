package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AsientoInvalidoException;
import com.example.aereopuerto.dto.AsientoDTO;
import com.example.aereopuerto.model.Asiento;
import com.example.aereopuerto.repository.AsientoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsientoService {

    private final AsientoRepository asientoRepository;


    public List<AsientoDTO> obtenerAsientosPorAvion(Integer avionId) {

        List<Asiento> asientos = asientoRepository.findByAvionId(avionId);

        return asientos.stream()
                .map(this::toDTO)
                .toList();
    }

    public Map<Integer, Boolean> verificarDisponibilidad(List<Integer> ids) {
        Map<Integer, Boolean> resultado = new HashMap<>();
        for (Integer id : ids) {
            Asiento asiento = asientoRepository.findById(id).orElse(null);
            resultado.put(id, asiento == null || Boolean.TRUE.equals(asiento.getOcupado()));
        }
        return resultado;
    }

    public List<Asiento> obtenerEconomicosDisponibles(Integer avionId) {
        Pattern pattern = Pattern.compile("(\\d+)");
        return asientoRepository.findAvailableByAvionId(avionId)
                .stream()
                .filter(a -> {
                    if (a.getCodigo() == null) return false;
                    Matcher m = pattern.matcher(a.getCodigo());
                    if (!m.find()) return false;
                    int fila = Integer.parseInt(m.group(1));
                    return fila >= 7;
                })
                .collect(Collectors.toList());
    }

    public Asiento asignarEconomicoAleatorio(Integer avionId) {
        List<Asiento> disponibles = obtenerEconomicosDisponibles(avionId);
        if (disponibles.isEmpty()) {
            throw new AsientoInvalidoException("No hay asientos economicos disponibles en este avion");
        }
        Collections.shuffle(disponibles);
        Asiento asiento = disponibles.get(0);
        asiento.setOcupado(true);
        return asientoRepository.save(asiento);
    }

    private AsientoDTO toDTO(Asiento asiento) {

        AsientoDTO dto = new AsientoDTO();

        dto.setId(asiento.getId());
        dto.setCodigo(asiento.getCodigo());
        dto.setOcupado(asiento.getOcupado());
        dto.setPrecioExtra(asiento.getPrecioExtra());

        if (asiento.getAvion() != null) {
            dto.setAvionId(asiento.getAvion().getId());
        }

        return dto;
    }
}

