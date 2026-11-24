package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.CarritoInvalidoException;
import com.example.aereopuerto.dto.CarritoItemDTO;
import com.example.aereopuerto.model.Carrito;
import com.example.aereopuerto.model.CarritoItem;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Vuelo;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.CarritoItemRepository;
import com.example.aereopuerto.repository.CarritoRepository;
import com.example.aereopuerto.repository.VueloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CarritoServiceTest {

    private static final int PERSONA_ID = 1;
    private static final int VUELO_ID = 10;

    @Mock
    private CarritoRepository carritoRepository;

    @Mock
    private CarritoItemRepository carritoItemRepository;

    @Mock
    private VueloRepository vueloRepository;

    @Mock
    private AsientoService asientoService;

    @InjectMocks
    private CarritoService carritoService;

    private Carrito carrito;
    private Vuelo vuelo;

    @BeforeEach
    void setUp() {
        carrito = Carrito.builder().id(5).persona(Persona.builder().id(PERSONA_ID).build()).build();
        vuelo = Vuelo.builder().id(VUELO_ID).estado(estadoVuelo.PROGRAMADO).build();
    }

    private CarritoItem itemEnCarrito(int id, int cantidad) {
        CarritoItem item = CarritoItem.builder()
                .id(id).carrito(carrito).vuelo(vuelo).cantidad(cantidad).claseVuelo(ClasesVuelo.ECONOMICA)
                .build();
        carrito.getItems().add(item);
        return item;
    }

    @Test
    @DisplayName("Agregar al carrito: permite hasta la cantidad de asientos libres")
    void agregarDentroDelCupo() {
        when(carritoRepository.findByPersonaId(PERSONA_ID)).thenReturn(Optional.of(carrito));
        when(vueloRepository.findById(VUELO_ID)).thenReturn(Optional.of(vuelo));
        when(asientoService.contarAsientosLibres(VUELO_ID)).thenReturn(3);
        when(carritoItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CarritoItemDTO dto = carritoService.addItem(PERSONA_ID, VUELO_ID, 3, ClasesVuelo.ECONOMICA);

        assertEquals(3, dto.getCantidad());
        assertEquals(3, dto.getAsientosDisponibles());
    }

    @Test
    @DisplayName("Agregar al carrito: rechaza sumar un pasaje si ya se pidieron todos los asientos libres")
    void agregarSuperaCupo() {
        itemEnCarrito(20, 3);
        when(carritoRepository.findByPersonaId(PERSONA_ID)).thenReturn(Optional.of(carrito));
        when(vueloRepository.findById(VUELO_ID)).thenReturn(Optional.of(vuelo));
        when(asientoService.contarAsientosLibres(VUELO_ID)).thenReturn(3);

        CarritoInvalidoException ex = assertThrows(CarritoInvalidoException.class,
                () -> carritoService.addItem(PERSONA_ID, VUELO_ID, 1, ClasesVuelo.ECONOMICA));

        assertEquals("Solo quedan 3 asientos disponibles en este vuelo.", ex.getMessage());
        verify(carritoItemRepository, never()).save(any());
    }

    @Test
    @DisplayName("Agregar al carrito: avisa cuando el vuelo no tiene asientos libres")
    void agregarVueloLleno() {
        when(carritoRepository.findByPersonaId(PERSONA_ID)).thenReturn(Optional.of(carrito));
        when(vueloRepository.findById(VUELO_ID)).thenReturn(Optional.of(vuelo));
        when(asientoService.contarAsientosLibres(VUELO_ID)).thenReturn(0);

        CarritoInvalidoException ex = assertThrows(CarritoInvalidoException.class,
                () -> carritoService.addItem(PERSONA_ID, VUELO_ID, 1, ClasesVuelo.ECONOMICA));

        assertEquals("No quedan asientos disponibles en este vuelo.", ex.getMessage());
    }

    @Test
    @DisplayName("Cambiar cantidad: no deja subir por encima de los asientos libres")
    void subirCantidadSuperaCupo() {
        CarritoItem item = itemEnCarrito(20, 3);
        when(carritoRepository.findByPersonaId(PERSONA_ID)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findById(20)).thenReturn(Optional.of(item));
        when(asientoService.contarAsientosLibres(VUELO_ID)).thenReturn(3);

        assertThrows(CarritoInvalidoException.class,
                () -> carritoService.updateItemQuantity(PERSONA_ID, 20, 4));
        assertEquals(3, item.getCantidad());
    }

    @Test
    @DisplayName("Cambiar cantidad: bajar siempre está permitido, aunque ya no queden asientos")
    void bajarCantidadSinValidar() {
        CarritoItem item = itemEnCarrito(20, 4);
        when(carritoRepository.findByPersonaId(PERSONA_ID)).thenReturn(Optional.of(carrito));
        when(carritoItemRepository.findById(20)).thenReturn(Optional.of(item));
        when(carritoItemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(asientoService.contarAsientosLibres(VUELO_ID)).thenReturn(1);

        CarritoItemDTO dto = carritoService.updateItemQuantity(PERSONA_ID, 20, 2);

        assertEquals(2, dto.getCantidad());
        assertEquals(1, dto.getAsientosDisponibles());
    }
}
