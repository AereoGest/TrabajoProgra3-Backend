package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AsientoInvalidoException;
import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.Exceptions.VueloInvalidoException;
import com.example.aereopuerto.dto.AsientoDTO;
import com.example.aereopuerto.model.Asiento;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.repository.AsientoRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import com.example.aereopuerto.repository.VueloRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AsientoService {

    // Distribución de cabina base para aviones (y precios extra por categoría)
    static final int ULTIMA_FILA_PRIMERA = 2;     // filas 1-2: primera clase, asientos A C E G
    static final int ULTIMA_FILA_EJECUTIVA = 6;   // filas 3-6: ejecutiva, asientos A-F
    private static final List<String> LETRAS_PRIMERA = List.of("A", "C", "E", "G");
    private static final List<String> LETRAS_RESTO = List.of("A", "B", "C", "D", "E", "F"); // fila 7+: económica
    static final double PRECIO_EXTRA_PRIMERA = 45000;
    static final double PRECIO_EXTRA_EJECUTIVA = 15000;
    static final double PRECIO_EXTRA_ECONOMICA = 0;

    private static final Pattern FILA_LETRA = Pattern.compile("^(\\d+)([A-Z]*)$");

    private final AsientoRepository asientoRepository;
    private final PasajeRepository pasajeRepository;
    private final VueloRepository vueloRepository;

    /** Configuración fija de cabina (letras y recargo) por cada categoría comercial. */
    record ConfigCategoria(ClasesVuelo categoria, String etiqueta, List<String> letras, double precioExtra) {
    }

    private static final List<ConfigCategoria> CATEGORIAS = List.of(
            new ConfigCategoria(ClasesVuelo.PRIMERA, "Primera", LETRAS_PRIMERA, PRECIO_EXTRA_PRIMERA),
            new ConfigCategoria(ClasesVuelo.BUSINESS, "Ejecutiva", LETRAS_RESTO, PRECIO_EXTRA_EJECUTIVA),
            new ConfigCategoria(ClasesVuelo.ECONOMICA, "Económica", LETRAS_RESTO, PRECIO_EXTRA_ECONOMICA)
    );

    /** Asiento a crear según la distribución de cabina. */
    record AsientoPlanificado(String codigo, double precioExtra, ClasesVuelo categoria) {
        AsientoPlanificado(String codigo, double precioExtra) {
            this(codigo, precioExtra, categoriaPorPrecio(precioExtra));
        }
    }

    private static ClasesVuelo categoriaPorPrecio(double precioExtra) {
        if (precioExtra >= 40000) return ClasesVuelo.PRIMERA;
        if (precioExtra >= PRECIO_EXTRA_EJECUTIVA) return ClasesVuelo.BUSINESS;
        return ClasesVuelo.ECONOMICA;
    }

    /** Determina la categoría efectiva de un asiento (con fallback para registros legacy sin columna categoría). */
    public static ClasesVuelo categoriaEfectiva(Asiento asiento) {
        if (asiento.getCategoria() != null) {
            return asiento.getCategoria() == ClasesVuelo.TURISTA ? ClasesVuelo.ECONOMICA : asiento.getCategoria();
        }
        if (asiento.getPrecioExtra() != null) {
            return categoriaPorPrecio(asiento.getPrecioExtra());
        }
        int fila = numeroFila(asiento.getCodigo());
        if (fila > 0 && fila <= ULTIMA_FILA_PRIMERA) return ClasesVuelo.PRIMERA;
        if (fila > 0 && fila <= ULTIMA_FILA_EJECUTIVA) return ClasesVuelo.BUSINESS;
        return ClasesVuelo.ECONOMICA;
    }

    /**
     * Genera, en orden de cabina base del avión, los primeros {@code cantidad} asientos:
     * 1A 1C 1E 1G 2A ... (primera), 3A..6F (ejecutiva), 7A en adelante (económica).
     */
    static List<AsientoPlanificado> distribucion(int cantidad) {
        List<AsientoPlanificado> plan = new ArrayList<>();
        int fila = 1;
        while (plan.size() < cantidad) {
            ClasesVuelo cat = fila <= ULTIMA_FILA_PRIMERA ? ClasesVuelo.PRIMERA
                    : fila <= ULTIMA_FILA_EJECUTIVA ? ClasesVuelo.BUSINESS
                    : ClasesVuelo.ECONOMICA;
            List<String> letras = fila <= ULTIMA_FILA_PRIMERA ? LETRAS_PRIMERA : LETRAS_RESTO;
            double precio = fila <= ULTIMA_FILA_PRIMERA ? PRECIO_EXTRA_PRIMERA
                    : fila <= ULTIMA_FILA_EJECUTIVA ? PRECIO_EXTRA_EJECUTIVA
                    : PRECIO_EXTRA_ECONOMICA;
            for (String letra : letras) {
                if (plan.size() == cantidad) break;
                plan.add(new AsientoPlanificado(fila + letra, precio, cat));
            }
            fila++;
        }
        return plan;
    }

    /**
     * Genera la distribución secuencial por filas para un vuelo según la cantidad
     * de asientos configurada en Primera, Ejecutiva y Económica (puede ser 0 en alguna).
     */
    public static List<AsientoPlanificado> distribucionVuelo(int primera, int ejecutiva, int economica) {
        List<AsientoPlanificado> plan = new ArrayList<>();
        int[] cantidades = { primera, ejecutiva, economica };
        int fila = 1;
        for (int i = 0; i < CATEGORIAS.size(); i++) {
            fila = agregarBloqueCategoria(plan, fila, cantidades[i], CATEGORIAS.get(i));
        }
        return plan;
    }

    private static int agregarBloqueCategoria(
            List<AsientoPlanificado> plan, int filaInicio, int cantidad, ConfigCategoria cfg) {
        int creados = 0;
        int fila = filaInicio;
        while (creados < cantidad) {
            for (String letra : cfg.letras()) {
                if (creados == cantidad) break;
                plan.add(new AsientoPlanificado(fila + letra, cfg.precioExtra(), cfg.categoria()));
                creados++;
            }
            fila++;
        }
        return fila;
    }

    /**
     * Verifica, sin modificar nada, que el avión pueda quedar con {@code nuevaCapacidad} asientos base.
     * Solo falla al achicar si no hay suficientes asientos libres (sin ocupar y sin pasaje) para quitar.
     */
    public void validarCambioCapacidad(Integer avionId, int nuevaCapacidad) {
        List<Asiento> actuales = asientoRepository.findByAvionIdAndVueloIsNull(avionId);
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
     * Deja al avión con tantos asientos base como su capacidad de pasajeros:
     * agrega los que faltan siguiendo la distribución de cabina y, si sobran,
     * quita los libres de las últimas filas. Nunca toca asientos ocupados o de vuelos específicos.
     */
    public void sincronizarAsientos(Avion avion) {
        int capacidad = avion.getCapacidadPasajeros();
        validarCambioCapacidad(avion.getId(), capacidad);

        List<Asiento> actuales = new ArrayList<>(asientoRepository.findByAvionIdAndVueloIsNull(avion.getId()));

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
                        .categoria(p.categoria())
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

    /** Valida sin tocar la BD que las cantidades por categoría sean válidas y entren en el avión. */
    public static void validarCantidadesBasicas(Vuelo vuelo) {
        int primera = valorNoNegativo(vuelo.getAsientosPrimera(), "Primera");
        int ejecutiva = valorNoNegativo(vuelo.getAsientosEjecutiva(), "Ejecutiva");
        int economica = valorNoNegativo(vuelo.getAsientosEconomica(), "Económica");
        int total = primera + ejecutiva + economica;

        if (total <= 0) {
            throw new VueloInvalidoException("El vuelo debe tener al menos 1 asiento entre sus categorías.");
        }
        int capacidadAvion = vuelo.getAvion() != null && vuelo.getAvion().getCapacidadPasajeros() != null
                ? vuelo.getAvion().getCapacidadPasajeros()
                : 0;
        if (capacidadAvion > 0 && total > capacidadAvion) {
            throw new VueloInvalidoException(
                    "La suma de asientos (" + total + ") supera la capacidad del avión (" + capacidadAvion + ").");
        }
    }

    /**
     * Valida la distribución de asientos por categoría de un vuelo (tanto en alta como en modificación)
     * sin escribir en la base de datos.
     */
    public void validarAsientosVuelo(Vuelo vuelo) {
        validarCantidadesBasicas(vuelo);
        int primera = vuelo.getAsientosPrimera();
        int ejecutiva = vuelo.getAsientosEjecutiva();
        int economica = vuelo.getAsientosEconomica();
        int total = primera + ejecutiva + economica;

        if (vuelo.getId() == null) {
            return;
        }

        long vendidos = pasajeRepository.countActivosByVueloId(vuelo.getId(), EstadoReserva.CANCELADA);
        if (total < vendidos) {
            throw new VueloInvalidoException(
                    "No se pueden configurar " + total + " asientos porque el vuelo ya tiene "
                            + vendidos + " pasaje(s) vendido(s).");
        }

        List<Asiento> actuales = asientoRepository.findByVueloId(vuelo.getId());
        if (actuales.isEmpty()) {
            return;
        }

        Integer avionActualId = actuales.stream()
                .map(Asiento::getAvion)
                .filter(Objects::nonNull)
                .map(Avion::getId)
                .findFirst()
                .orElse(null);

        boolean hayBloqueados = actuales.stream().anyMatch(a -> !sePuedeQuitar(a));
        if (avionActualId != null && vuelo.getAvion() != null
                && !Objects.equals(avionActualId, vuelo.getAvion().getId()) && hayBloqueados) {
            throw new VueloInvalidoException(
                    "No se puede cambiar el avión porque el vuelo ya tiene asientos ocupados o asignados a un pasaje.");
        }

        Map<ClasesVuelo, Integer> deseados = Map.of(
                ClasesVuelo.PRIMERA, primera,
                ClasesVuelo.BUSINESS, ejecutiva,
                ClasesVuelo.ECONOMICA, economica
        );
        for (ConfigCategoria cfg : CATEGORIAS) {
            int objetivo = deseados.get(cfg.categoria());
            long bloqueadosCategoria = actuales.stream()
                    .filter(a -> categoriaEfectiva(a) == cfg.categoria() && !sePuedeQuitar(a))
                    .count();
            if (objetivo < bloqueadosCategoria) {
                throw new VueloInvalidoException(
                        "No se pueden reducir los asientos de " + cfg.etiqueta() + " a " + objetivo
                                + " porque ya hay " + bloqueadosCategoria + " ocupado(s) o asignado(s).");
            }
        }
    }

    /**
     * Genera o actualiza los asientos propios de un vuelo según sus cantidades por categoría.
     * Si todos los asientos actuales del vuelo están libres, regenera la distribución limpia;
     * si hay ocupados, ajusta por categoría sin tocarlos.
     */
    public void sincronizarAsientosVuelo(Vuelo vuelo) {
        validarAsientosVuelo(vuelo);

        int primera = vuelo.getAsientosPrimera();
        int ejecutiva = vuelo.getAsientosEjecutiva();
        int economica = vuelo.getAsientosEconomica();

        List<Asiento> actuales = new ArrayList<>(asientoRepository.findByVueloId(vuelo.getId()));
        if (actuales.isEmpty() || actuales.stream().allMatch(this::sePuedeQuitar)) {
            if (!actuales.isEmpty()) {
                asientoRepository.deleteAll(actuales);
            }
            List<Asiento> nuevos = distribucionVuelo(primera, ejecutiva, economica).stream()
                    .map(p -> Asiento.builder()
                            .codigo(p.codigo())
                            .precioExtra(p.precioExtra())
                            .avion(vuelo.getAvion())
                            .vuelo(vuelo)
                            .categoria(p.categoria())
                            .ocupado(false)
                            .build())
                    .toList();
            asientoRepository.saveAll(nuevos);
            return;
        }

        ajustarAsientosPreservandoOcupados(vuelo, actuales, primera, ejecutiva, economica);
    }

    private void ajustarAsientosPreservandoOcupados(
            Vuelo vuelo, List<Asiento> actuales, int primera, int ejecutiva, int economica) {
        Map<ClasesVuelo, Integer> objetivos = Map.of(
                ClasesVuelo.PRIMERA, primera,
                ClasesVuelo.BUSINESS, ejecutiva,
                ClasesVuelo.ECONOMICA, economica
        );

        Map<ClasesVuelo, List<Asiento>> porCategoria = new EnumMap<>(ClasesVuelo.class);
        for (ConfigCategoria cfg : CATEGORIAS) {
            porCategoria.put(cfg.categoria(), new ArrayList<>());
        }
        for (Asiento a : actuales) {
            porCategoria.computeIfAbsent(categoriaEfectiva(a), k -> new ArrayList<>()).add(a);
        }

        List<Asiento> aEliminar = new ArrayList<>();
        List<Asiento> conservados = new ArrayList<>();

        for (ConfigCategoria cfg : CATEGORIAS) {
            List<Asiento> lista = porCategoria.get(cfg.categoria());
            int objetivo = objetivos.get(cfg.categoria());
            if (lista.size() > objetivo) {
                int sobran = lista.size() - objetivo;
                List<Asiento> quitables = lista.stream()
                        .filter(this::sePuedeQuitar)
                        .sorted(Comparator.comparing(AsientoService::claveOrden).reversed())
                        .limit(sobran)
                        .toList();
                Set<Asiento> quitadosSet = Collections.newSetFromMap(new IdentityHashMap<>());
                quitadosSet.addAll(quitables);
                aEliminar.addAll(quitables);
                lista.stream().filter(a -> !quitadosSet.contains(a)).forEach(conservados::add);
            } else {
                conservados.addAll(lista);
            }
        }

        Set<String> codigosUsados = conservados.stream()
                .map(Asiento::getCodigo)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Asiento> nuevos = new ArrayList<>();
        int filaSugerida = 1;

        for (ConfigCategoria cfg : CATEGORIAS) {
            List<Asiento> existentesCat = conservados.stream()
                    .filter(a -> categoriaEfectiva(a) == cfg.categoria())
                    .toList();
            int faltan = objetivos.get(cfg.categoria()) - existentesCat.size();

            Set<Integer> filasOtras = conservados.stream()
                    .filter(a -> categoriaEfectiva(a) != cfg.categoria())
                    .map(a -> numeroFila(a.getCodigo()))
                    .filter(f -> f > 0)
                    .collect(Collectors.toSet());

            int maxFilaPropia = existentesCat.stream()
                    .mapToInt(a -> numeroFila(a.getCodigo()))
                    .max()
                    .orElse(0);

            if (faltan <= 0) {
                filaSugerida = Math.max(filaSugerida, maxFilaPropia + 1);
                continue;
            }

            int inicio = existentesCat.isEmpty()
                    ? filaSugerida
                    : existentesCat.stream().mapToInt(a -> numeroFila(a.getCodigo())).filter(f -> f > 0).min().orElse(filaSugerida);

            int fila = Math.max(1, inicio);
            int agregados = 0;
            while (agregados < faltan) {
                if (!filasOtras.contains(fila)) {
                    for (String letra : cfg.letras()) {
                        if (agregados == faltan) break;
                        String codigo = fila + letra;
                        if (codigosUsados.add(codigo)) {
                            nuevos.add(Asiento.builder()
                                    .codigo(codigo)
                                    .precioExtra(cfg.precioExtra())
                                    .avion(vuelo.getAvion())
                                    .vuelo(vuelo)
                                    .categoria(cfg.categoria())
                                    .ocupado(false)
                                    .build());
                            agregados++;
                        }
                    }
                }
                fila++;
            }
            filaSugerida = Math.max(filaSugerida, Math.max(maxFilaPropia + 1, fila));
        }

        if (!aEliminar.isEmpty()) {
            asientoRepository.deleteAll(aEliminar);
        }
        if (!nuevos.isEmpty()) {
            asientoRepository.saveAll(nuevos);
        }
    }

    private static int valorNoNegativo(Integer valor, String categoria) {
        if (valor == null) {
            throw new VueloInvalidoException("Indicá la cantidad de asientos para " + categoria + ".");
        }
        if (valor < 0) {
            throw new VueloInvalidoException("La cantidad de asientos de " + categoria + " no puede ser negativa.");
        }
        return valor;
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

    private static int numeroFila(String codigo) {
        if (codigo == null) return -1;
        Matcher m = FILA_LETRA.matcher(codigo);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    private static int ordenCategoria(ClasesVuelo categoria) {
        if (categoria == ClasesVuelo.PRIMERA) return 0;
        if (categoria == ClasesVuelo.BUSINESS) return 1;
        return 2;
    }

    public List<AsientoDTO> obtenerAsientosPorAvion(Integer avionId) {
        List<Asiento> asientos = asientoRepository.findByAvionIdAndVueloIsNull(avionId);

        return asientos.stream()
                .sorted(Comparator.comparingInt((Asiento a) -> ordenCategoria(categoriaEfectiva(a)))
                        .thenComparing(AsientoService::claveOrden))
                .map(a -> toDTO(a, Boolean.TRUE.equals(a.getOcupado())))
                .toList();
    }

    /**
     * Devuelve los asientos reales de un vuelo junto con su categoría y ocupación actual en ese vuelo.
     * Para vuelos previos a esta configuración (sin asientos propios), usa los asientos base del avión
     * y calcula la ocupación por los pasajes activos del vuelo.
     */
    public List<AsientoDTO> obtenerAsientosPorVuelo(Integer vueloId) {
        Vuelo vuelo = vueloRepository.findById(vueloId)
                .orElseThrow(() -> new VueloInvalidoException("Vuelo no encontrado. ID: " + vueloId));

        List<Asiento> asientosVuelo = asientoRepository.findByVueloId(vueloId);
        Set<Integer> ocupadosEnVuelo = new HashSet<>(
                pasajeRepository.findAsientosOcupadosPorVuelo(vueloId, EstadoReserva.CANCELADA));

        boolean usaPropios = !asientosVuelo.isEmpty();
        List<Asiento> origen = usaPropios
                ? asientosVuelo
                : (vuelo.getAvion() != null
                        ? asientoRepository.findByAvionIdAndVueloIsNull(vuelo.getAvion().getId())
                        : List.of());

        return origen.stream()
                .sorted(Comparator.comparingInt((Asiento a) -> ordenCategoria(categoriaEfectiva(a)))
                        .thenComparing(AsientoService::claveOrden))
                .map(a -> toDTO(a, usaPropios
                        ? (Boolean.TRUE.equals(a.getOcupado()) || ocupadosEnVuelo.contains(a.getId()))
                        : ocupadosEnVuelo.contains(a.getId())))
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
        return asientoRepository.findAvailableByAvionId(avionId)
                .stream()
                .filter(a -> categoriaEfectiva(a) == ClasesVuelo.ECONOMICA)
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

    public Asiento asignarAleatorioParaVuelo(Vuelo vuelo, ClasesVuelo clasePreferida) {
        List<Asiento> candidatos = asientoRepository.findByVueloId(vuelo.getId());
        boolean usaPropios = !candidatos.isEmpty();
        if (!usaPropios && vuelo.getAvion() != null) {
            candidatos = asientoRepository.findByAvionIdAndVueloIsNull(vuelo.getAvion().getId());
        }
        Set<Integer> ocupadosEnVuelo = new HashSet<>(
                pasajeRepository.findAsientosOcupadosPorVuelo(vuelo.getId(), EstadoReserva.CANCELADA));

        List<Asiento> libres = candidatos.stream()
                .filter(a -> !ocupadosEnVuelo.contains(a.getId())
                        && (!usaPropios || !Boolean.TRUE.equals(a.getOcupado())))
                .toList();

        if (libres.isEmpty()) {
            throw new AsientoInvalidoException("No hay asientos disponibles en este vuelo");
        }

        ClasesVuelo objetivo = clasePreferida == ClasesVuelo.TURISTA ? ClasesVuelo.ECONOMICA
                : (clasePreferida != null ? clasePreferida : ClasesVuelo.ECONOMICA);

        List<Asiento> porClase = libres.stream()
                .filter(a -> categoriaEfectiva(a) == objetivo)
                .toList();
        if (porClase.isEmpty() && objetivo != ClasesVuelo.ECONOMICA) {
            porClase = libres.stream()
                    .filter(a -> categoriaEfectiva(a) == ClasesVuelo.ECONOMICA)
                    .toList();
        }
        List<Asiento> pool = new ArrayList<>(porClase.isEmpty() ? libres : porClase);
        Collections.shuffle(pool);
        Asiento elegido = pool.get(0);
        if (usaPropios) {
            elegido.setOcupado(true);
            elegido = asientoRepository.save(elegido);
        }
        return elegido;
    }

    private AsientoDTO toDTO(Asiento asiento, boolean ocupado) {
        AsientoDTO dto = new AsientoDTO();
        dto.setId(asiento.getId());
        dto.setCodigo(asiento.getCodigo());
        dto.setOcupado(ocupado);
        dto.setPrecioExtra(asiento.getPrecioExtra());
        dto.setCategoria(categoriaEfectiva(asiento));
        if (asiento.getAvion() != null) {
            dto.setAvionId(asiento.getAvion().getId());
        }
        if (asiento.getVuelo() != null) {
            dto.setVueloId(asiento.getVuelo().getId());
        }
        return dto;
    }
}

