package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AsientoInvalidoException;
import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.dto.AsientoDTO;
import com.example.aereopuerto.model.Asiento;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.repository.AsientoRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsientoService {

    // Distribución de cabina (la misma que dibuja el mapa de asientos del front)
    static final int ULTIMA_FILA_PRIMERA = 2;     // filas 1-2: primera clase, asientos A C E G
    static final int ULTIMA_FILA_EJECUTIVA = 6;   // filas 3-6: ejecutiva, asientos A-F
    private static final List<String> LETRAS_PRIMERA = List.of("A", "C", "E", "G");
    private static final List<String> LETRAS_RESTO = List.of("A", "B", "C", "D", "E", "F"); // fila 7+: económica
    static final double PRECIO_EXTRA_PRIMERA = 45000;
    static final double PRECIO_EXTRA_EJECUTIVA = 15000;
    static final double PRECIO_EXTRA_ECONOMICA = 5000;

    private static final Pattern FILA_LETRA = Pattern.compile("^(\\d+)([A-Z]*)$");

    private final AsientoRepository asientoRepository;
    private final PasajeRepository pasajeRepository;

    /** Asiento a crear según la distribución de cabina. */
    record AsientoPlanificado(String codigo, double precioExtra) {
    }

    /**
     * Genera, en orden de cabina, los primeros {@code cantidad} asientos:
     * 1A 1C 1E 1G 2A ... (primera), 3A..6F (ejecutiva), 7A en adelante (económica).
     */
    static List<AsientoPlanificado> distribucion(int cantidad) {
        List<AsientoPlanificado> plan = new ArrayList<>();
        int fila = 1;
        while (plan.size() < cantidad) {
            List<String> letras = fila <= ULTIMA_FILA_PRIMERA ? LETRAS_PRIMERA : LETRAS_RESTO;
            double precio = fila <= ULTIMA_FILA_PRIMERA ? PRECIO_EXTRA_PRIMERA
                    : fila <= ULTIMA_FILA_EJECUTIVA ? PRECIO_EXTRA_EJECUTIVA
                    : PRECIO_EXTRA_ECONOMICA;
            for (String letra : letras) {
                if (plan.size() == cantidad) break;
                plan.add(new AsientoPlanificado(fila + letra, precio));
            }
            fila++;
        }
        return plan;
    }

    /**
     * Verifica, sin modificar nada, que el avión pueda quedar con {@code nuevaCapacidad} asientos.
     * Solo falla al achicar si no hay suficientes asientos libres (sin ocupar y sin pasaje) para quitar.
     */
    public void validarCambioCapacidad(Integer avionId, int nuevaCapacidad) {
        List<Asiento> actuales = asientoRepository.findByAvionId(avionId);
        int sobrantes = actuales.size() - nuevaCapacidad;
        if (sobrantes <= 0) return;

        long libres = actuales.stream().filter(this::sePuedeQuitar).count();
        if (libres < sobrantes) {
            throw new AvionInvalidoException(
                    "No se puede reducir la capacidad a " + nuevaCapacidad + ": habría que quitar " + sobrantes
                            + " asiento(s) y solo " + libres + " están libres (el resto está ocupado o asignado a un pasaje).");
        }
    }

    /**
     * Deja al avión con tantos asientos como su capacidad de pasajeros:
     * agrega los que faltan siguiendo la distribución de cabina y, si sobran,
     * quita los libres de las últimas filas. Nunca toca asientos ocupados o con pasaje.
     */
    public void sincronizarAsientos(Avion avion) {
        int capacidad = avion.getCapacidadPasajeros();
        validarCambioCapacidad(avion.getId(), capacidad);

        List<Asiento> actuales = new ArrayList<>(asientoRepository.findByAvionId(avion.getId()));

        if (actuales.size() < capacidad) {
            Set<String> existentes = actuales.stream()
                    .map(Asiento::getCodigo)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            int faltan = capacidad - actuales.size();
            List<Asiento> nuevos = new ArrayList<>();
            // Se planifica de más por si el avión ya tiene códigos fuera de la distribución
            for (AsientoPlanificado p : distribucion(capacidad + actuales.size())) {
                if (nuevos.size() == faltan) break;
                if (existentes.contains(p.codigo())) continue;
                nuevos.add(Asiento.builder()
                        .codigo(p.codigo())
                        .precioExtra(p.precioExtra())
                        .avion(avion)
                        .ocupado(false)
                        .build());
            }
            asientoRepository.saveAll(nuevos);
        } else if (actuales.size() > capacidad) {
            int sobrantes = actuales.size() - capacidad;
            // Se quitan primero los de las últimas filas
            List<Asiento> aQuitar = actuales.stream()
                    .filter(this::sePuedeQuitar)
                    .sorted(Comparator.comparing(AsientoService::claveOrden).reversed())
                    .limit(sobrantes)
                    .toList();
            asientoRepository.deleteAll(aQuitar);
        }
    }

    private boolean sePuedeQuitar(Asiento asiento) {
        return !Boolean.TRUE.equals(asiento.getOcupado()) && !pasajeRepository.existsByAsientoId(asiento.getId());
    }

    /** Orden de cabina: fila y después letra ("12B" → 12, "B"). */
    private static String claveOrden(Asiento asiento) {
        Matcher m = FILA_LETRA.matcher(asiento.getCodigo() == null ? "" : asiento.getCodigo());
        if (!m.matches()) return "";
        return String.format("%05d%s", Integer.parseInt(m.group(1)), m.group(2));
    }


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

