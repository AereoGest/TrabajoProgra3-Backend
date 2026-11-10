package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.AvionInvalidoException;
import com.example.aereopuerto.model.Asiento;
import com.example.aereopuerto.model.Avion;
import com.example.aereopuerto.repository.AsientoRepository;
import com.example.aereopuerto.repository.PasajeRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;

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
        when(asientoRepository.findByAvionId(7)).thenReturn(List.of());
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
        when(asientoRepository.findByAvionId(7)).thenReturn(new ArrayList<>(List.of(
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
        when(asientoRepository.findByAvionId(7)).thenReturn(actuales);
        when(pasajeRepository.existsByAsientoId(anyInt())).thenReturn(false);

        asientoService.sincronizarAsientos(avion(2));

        verify(asientoRepository).deleteAll(List.of(actuales.get(2), actuales.get(3)));   // 10A y 9F
        verify(asientoRepository, never()).saveAll(any());
    }

    @Test
    @DisplayName("No se puede achicar si no alcanzan los asientos libres (ocupados o con pasaje)")
    void validarCambioCapacidad_sinLibresSuficientes_lanzaExcepcion() {
        when(asientoRepository.findByAvionId(7)).thenReturn(List.of(
                asiento(1, "7A", true), asiento(2, "7B", false), asiento(3, "7C", false)));
        when(pasajeRepository.existsByAsientoId(2)).thenReturn(true);   // 7B tiene pasaje
        when(pasajeRepository.existsByAsientoId(3)).thenReturn(false);

        assertThrows(AvionInvalidoException.class, () -> asientoService.validarCambioCapacidad(7, 1));
        verify(asientoRepository, never()).deleteAll(any());
    }
}
