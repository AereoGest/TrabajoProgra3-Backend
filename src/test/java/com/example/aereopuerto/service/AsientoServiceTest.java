package com.example.aereopuerto.service;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AsientoServiceTest {

    @Mock
    private AsientoRepository asientoRepository;

    @Mock
    private PasajeRepository pasajeRepository;

    @Mock
    private VueloRepository vueloRepository;

    @InjectMocks
    private AsientoService asientoService;

    private Avion avion(int capacidad) {
        return Avion.builder().id(7).identificador("LV-A7").capacidadPasajeros(capacidad).build();
    }

    private Asiento asiento(int id, String codigo, boolean ocupado) {
        return Asiento.builder().id(id).codigo(codigo).ocupado(ocupado).precioExtra(0.0).build();
    }

    @SuppressWarnings("unchecked")
    private List<Asiento> capturarGuardados() {
        ArgumentCaptor<List<Asiento>> captor = ArgumentCaptor.forClass(List.class);
        verify(asientoRepository).saveAll(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("La distribución sigue el mapa de cabina: primera (1-2), ejecutiva (3-6) y económica (7+)")
    void distribucion_respetaCabinas() {
        List<AsientoService.AsientoPlanificado> plan = AsientoService.distribucion(40);

        assertEquals(40, plan.size());
        assertEquals("1A", plan.get(0).codigo());
        assertEquals("1C", plan.get(1).codigo());
        assertEquals(AsientoService.PRECIO_EXTRA_PRIMERA, plan.get(0).precioExtra());
        assertEquals("3A", plan.get(8).codigo());                     // 8 asientos de primera
        assertEquals(AsientoService.PRECIO_EXTRA_EJECUTIVA, plan.get(8).precioExtra());
        assertEquals("7A", plan.get(32).codigo());                    // + 24 de ejecutiva
        assertEquals(AsientoService.PRECIO_EXTRA_ECONOMICA, plan.get(32).precioExtra());
        assertEquals("8B", plan.get(39).codigo());
    }

    @Test
    @DisplayName("Avión nuevo: se crean tantos asientos como su capacidad, todos libres")
    void sincronizar_avionNuevo_creaTodos() {
        when(asientoRepository.findByAvionIdAndVueloIsNull(7)).thenReturn(List.of());
        Avion avion = avion(180);

        asientoService.sincronizarAsientos(avion);

        List<Asiento> guardados = capturarGuardados();
        assertEquals(180, guardados.size());
        assertTrue(guardados.stream().allMatch(a -> a.getAvion() == avion && Boolean.FALSE.equals(a.getOcupado())));
        assertEquals(180, guardados.stream().map(Asiento::getCodigo).distinct().count());
    }

    @Test
    @DisplayName("Avión con algunos asientos: completa solo los que faltan sin repetir códigos")
    void sincronizar_completaFaltantes() {
        when(asientoRepository.findByAvionIdAndVueloIsNull(7)).thenReturn(new ArrayList<>(List.of(
                asiento(1, "1A", true), asiento(2, "1B", false), asiento(3, "12A", false))));

        asientoService.sincronizarAsientos(avion(50));

        List<Asiento> guardados = capturarGuardados();
        assertEquals(47, guardados.size());
        assertTrue(guardados.stream().noneMatch(a -> a.getCodigo().equals("1A") || a.getCodigo().equals("12A")));
    }

    @Test
    @DisplayName("Al achicar se quitan los asientos libres de las últimas filas")
    void sincronizar_achicar_quitaUltimasFilasLibres() {
        List<Asiento> actuales = List.of(
                asiento(1, "7A", false), asiento(2, "7B", false), asiento(3, "10A", false), asiento(4, "9F", false));
        when(asientoRepository.findByAvionIdAndVueloIsNull(7)).thenReturn(actuales);
        when(pasajeRepository.existsByAsientoId(anyInt())).thenReturn(false);

        asientoService.sincronizarAsientos(avion(2));

        verify(asientoRepository).deleteAll(List.of(actuales.get(2), actuales.get(3)));   // 10A y 9F
        verify(asientoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("No se puede achicar si no alcanzan los asientos libres (ocupados o con pasaje)")
    void validarCambioCapacidad_sinLibresSuficientes_lanzaExcepcion() {
        when(asientoRepository.findByAvionIdAndVueloIsNull(7)).thenReturn(List.of(
                asiento(1, "7A", true), asiento(2, "7B", false), asiento(3, "7C", false)));
        when(pasajeRepository.existsByAsientoId(2)).thenReturn(true);   // 7B tiene pasaje
        when(pasajeRepository.existsByAsientoId(3)).thenReturn(false);

        assertThrows(AvionInvalidoException.class, () -> asientoService.validarCambioCapacidad(7, 1));
        verify(asientoRepository, never()).deleteAll(any());
    }

    // ───────────────────────── Asientos por vuelo ─────────────────────────

    @Test
    @DisplayName("La distribución por vuelo respeta cantidades configuradas y omite filas de categorías en 0")
    void distribucionVuelo_respetaCantidadesYFilas() {
        List<AsientoService.AsientoPlanificado> plan = AsientoService.distribucionVuelo(0, 6, 8);

        assertEquals(14, plan.size());
        assertEquals(6, plan.stream().filter(a -> a.categoria() == ClasesVuelo.BUSINESS).count());
        assertEquals(8, plan.stream().filter(a -> a.categoria() == ClasesVuelo.ECONOMICA).count());
        assertEquals("1A", plan.get(0).codigo());
        assertEquals(ClasesVuelo.BUSINESS, plan.get(0).categoria());
        assertEquals("2A", plan.get(6).codigo());
        assertEquals(ClasesVuelo.ECONOMICA, plan.get(6).categoria());
    }

    @Test
    @DisplayName("Vuelo nuevo: genera asientos vinculados al vuelo con su categoría y precio extra")
    void sincronizarVuelo_nuevo_creaAsientosPorCategoria() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(10).avion(av)
                .asientosPrimera(4).asientosEjecutiva(6).asientosEconomica(10).build();

        when(pasajeRepository.countActivosByVueloId(10, EstadoReserva.CANCELADA)).thenReturn(0L);
        when(asientoRepository.findByVueloId(10)).thenReturn(List.of());

        asientoService.sincronizarAsientosVuelo(vuelo);

        List<Asiento> guardados = capturarGuardados();
        assertEquals(20, guardados.size());
        assertEquals(4, guardados.stream().filter(a -> a.getCategoria() == ClasesVuelo.PRIMERA).count());
        assertEquals(6, guardados.stream().filter(a -> a.getCategoria() == ClasesVuelo.BUSINESS).count());
        assertEquals(10, guardados.stream().filter(a -> a.getCategoria() == ClasesVuelo.ECONOMICA).count());
        assertTrue(guardados.stream().allMatch(a -> a.getVuelo() == vuelo && a.getAvion() == av));
    }

    @Test
    @DisplayName("Modificar vuelo: impide reducir categoría por debajo de asientos ocupados")
    void validarAsientosVuelo_reduceDebajoDeOcupados_lanzaExcepcion() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(10).avion(av)
                .asientosPrimera(1).asientosEjecutiva(6).asientosEconomica(10).build();

        Asiento p1 = Asiento.builder().id(1).codigo("1A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.PRIMERA).ocupado(true).precioExtra(45000.0).build();
        Asiento p2 = Asiento.builder().id(2).codigo("1C").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.PRIMERA).ocupado(true).precioExtra(45000.0).build();

        when(pasajeRepository.countActivosByVueloId(10, EstadoReserva.CANCELADA)).thenReturn(2L);
        when(asientoRepository.findByVueloId(10)).thenReturn(List.of(p1, p2));

        VueloInvalidoException ex = assertThrows(VueloInvalidoException.class,
                () -> asientoService.validarAsientosVuelo(vuelo));
        assertTrue(ex.getMessage().contains("Primera"));
    }

    @Test
    @DisplayName("obtenerAsientosPorVuelo usa los asientos del vuelo cuando existen y calcula ocupación real")
    void obtenerAsientosPorVuelo_conPropios_devuelveCategoriaYOcupacion() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(10).avion(av).build();

        Asiento a1 = Asiento.builder().id(101).codigo("1A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.PRIMERA).precioExtra(45000.0).ocupado(false).build();
        Asiento a2 = Asiento.builder().id(102).codigo("2A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.ECONOMICA).precioExtra(0.0).ocupado(false).build();

        when(vueloRepository.findById(10)).thenReturn(Optional.of(vuelo));
        when(asientoRepository.findByVueloId(10)).thenReturn(List.of(a2, a1));
        when(pasajeRepository.findAsientosOcupadosPorVuelo(10, EstadoReserva.CANCELADA)).thenReturn(List.of(102));

        List<AsientoDTO> resultado = asientoService.obtenerAsientosPorVuelo(10);

        assertEquals(2, resultado.size());
        assertEquals("1A", resultado.get(0).getCodigo());
        assertEquals(ClasesVuelo.PRIMERA, resultado.get(0).getCategoria());
        assertEquals(AsientoService.PRECIO_EXTRA_PRIMERA, resultado.get(0).getPrecioExtra());
        assertFalse(resultado.get(0).getOcupado());
        assertEquals("2A", resultado.get(1).getCodigo());
        assertEquals(ClasesVuelo.ECONOMICA, resultado.get(1).getCategoria());
        assertEquals(AsientoService.PRECIO_EXTRA_ECONOMICA, resultado.get(1).getPrecioExtra());
        assertTrue(resultado.get(1).getOcupado());
    }

    @Test
    @DisplayName("Aplica tarifas vigentes en las 3 categorías incluso sobre asientos legacy guardados con precios viejos")
    void obtenerAsientosPorVuelo_asientosLegacyConPreciosViejos_aplicaTarifasVigentes() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(20).avion(av).build();

        Asiento pri = Asiento.builder().id(1).codigo("1A").avion(av).precioExtra(45000.0).ocupado(false).build();
        Asiento eje = Asiento.builder().id(2).codigo("2A").avion(av).precioExtra(25000.0).ocupado(false).build();
        Asiento eco = Asiento.builder().id(3).codigo("3A").avion(av).precioExtra(0.0).ocupado(false).build();
        Asiento ecoTarifaActual = Asiento.builder().id(4).codigo("4A").avion(av)
                .precioExtra(AsientoService.PRECIO_EXTRA_ECONOMICA).ocupado(false).build();

        when(vueloRepository.findById(20)).thenReturn(Optional.of(vuelo));
        when(asientoRepository.findByVueloId(20)).thenReturn(List.of());
        when(asientoRepository.findByAvionIdAndVueloIsNull(7)).thenReturn(List.of(ecoTarifaActual, eco, eje, pri));
        when(pasajeRepository.findAsientosOcupadosPorVuelo(20, EstadoReserva.CANCELADA)).thenReturn(List.of());

        List<AsientoDTO> resultado = asientoService.obtenerAsientosPorVuelo(20);

        assertEquals(ClasesVuelo.PRIMERA, resultado.get(0).getCategoria());
        assertEquals(AsientoService.PRECIO_EXTRA_PRIMERA, resultado.get(0).getPrecioExtra());
        assertEquals(ClasesVuelo.BUSINESS, resultado.get(1).getCategoria());
        assertEquals(AsientoService.PRECIO_EXTRA_EJECUTIVA, resultado.get(1).getPrecioExtra());
        assertEquals(ClasesVuelo.ECONOMICA, resultado.get(2).getCategoria());
        assertEquals(AsientoService.PRECIO_EXTRA_ECONOMICA, resultado.get(2).getPrecioExtra());
        assertEquals(ClasesVuelo.ECONOMICA, resultado.get(3).getCategoria());
        assertEquals(AsientoService.PRECIO_EXTRA_ECONOMICA, resultado.get(3).getPrecioExtra());
    }

    @Test
    @DisplayName("asignarAleatorioParaVuelo siempre asigna un asiento de clase turista (Económica)")
    void asignarAleatorioParaVuelo_siempreEligeEconomica() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(10).avion(av).build();

        Asiento primera = Asiento.builder().id(1).codigo("1A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.PRIMERA).precioExtra(45000.0).ocupado(false).build();
        Asiento eco = Asiento.builder().id(2).codigo("3A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.ECONOMICA).precioExtra(5000.0).ocupado(false).build();

        when(asientoRepository.findByVueloId(10)).thenReturn(List.of(primera, eco));
        when(pasajeRepository.findAsientosOcupadosPorVuelo(10, EstadoReserva.CANCELADA)).thenReturn(List.of());
        when(asientoRepository.save(any(Asiento.class))).thenAnswer(inv -> inv.getArgument(0));

        Asiento asignado = asientoService.asignarAleatorioParaVuelo(vuelo, ClasesVuelo.PRIMERA);

        assertEquals(2, asignado.getId());
        assertEquals(ClasesVuelo.ECONOMICA, asignado.getCategoria());
        assertTrue(asignado.getOcupado());
    }

    @Test
    @DisplayName("obtenerAsientosPorVuelo para vuelos legacy hace fallback al avión y usa ocupación por pasaje del vuelo")
    void obtenerAsientosPorVuelo_legacyFallback_aAvion() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(15).avion(av).build();

        Asiento legacy1 = Asiento.builder().id(5).codigo("1A").avion(av).precioExtra(45000.0).ocupado(true).build();
        Asiento legacy2 = Asiento.builder().id(6).codigo("3A").avion(av).precioExtra(0.0).ocupado(false).build();

        when(vueloRepository.findById(15)).thenReturn(Optional.of(vuelo));
        when(asientoRepository.findByVueloId(15)).thenReturn(List.of());
        when(asientoRepository.findByAvionIdAndVueloIsNull(7)).thenReturn(List.of(legacy1, legacy2));
        when(pasajeRepository.findAsientosOcupadosPorVuelo(15, EstadoReserva.CANCELADA)).thenReturn(List.of(6));

        List<AsientoDTO> resultado = asientoService.obtenerAsientosPorVuelo(15);

        assertEquals(2, resultado.size());
        // legacy1 marcado ocupado en otro vuelo no bloquea este vuelo si no tiene pasaje en vuelo 15
        assertEquals("1A", resultado.get(0).getCodigo());
        assertFalse(resultado.get(0).getOcupado());
        assertEquals("3A", resultado.get(1).getCodigo());
        assertEquals(ClasesVuelo.ECONOMICA, resultado.get(1).getCategoria());
        assertTrue(resultado.get(1).getOcupado());
    }

    @Test
    @DisplayName("Modificar vuelo con asientos ocupados: conserva los ocupados y ajusta cantidades sin colisiones")
    void sincronizarVuelo_conOcupados_preservaOcupadosYSinColisiones() {
        Avion av = avion(100);
        Vuelo vuelo = Vuelo.builder().id(10).avion(av)
                .asientosPrimera(1).asientosEjecutiva(2).asientosEconomica(2).build();

        Asiento p1 = Asiento.builder().id(1).codigo("1A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.PRIMERA).ocupado(true).precioExtra(45000.0).build();
        Asiento p2 = Asiento.builder().id(2).codigo("1C").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.PRIMERA).ocupado(false).precioExtra(45000.0).build();
        Asiento e1 = Asiento.builder().id(3).codigo("2A").avion(av).vuelo(vuelo)
                .categoria(ClasesVuelo.ECONOMICA).ocupado(false).precioExtra(0.0).build();

        when(pasajeRepository.countActivosByVueloId(10, EstadoReserva.CANCELADA)).thenReturn(1L);
        when(asientoRepository.findByVueloId(10)).thenReturn(List.of(p1, p2, e1));
        when(pasajeRepository.existsByAsientoId(anyInt())).thenReturn(false);

        asientoService.sincronizarAsientosVuelo(vuelo);

        verify(asientoRepository).deleteAll(List.of(p2));
        List<Asiento> guardados = capturarGuardados();
        assertEquals(3, guardados.size()); // +2 Ejecutiva +1 Económica
        assertEquals(2, guardados.stream().filter(a -> a.getCategoria() == ClasesVuelo.BUSINESS).count());
        assertEquals(1, guardados.stream().filter(a -> a.getCategoria() == ClasesVuelo.ECONOMICA).count());
        // Ningún código nuevo colisiona con 1A o 2A ni mezcla fila 2 (de Económica) con Ejecutiva
        assertTrue(guardados.stream().noneMatch(a -> a.getCodigo().equals("1A") || a.getCodigo().equals("2A")));
        assertTrue(guardados.stream()
                .filter(a -> a.getCategoria() == ClasesVuelo.BUSINESS)
                .noneMatch(a -> a.getCodigo().startsWith("2")));
    }
}
