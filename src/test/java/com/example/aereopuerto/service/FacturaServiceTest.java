package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.FacturaInvalidaException;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.model.Factura;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.repository.FacturaRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FacturaServiceTest {

    private static final int FACTURA_ID = 50;

    @Mock
    private FacturaRepository facturaRepository;

    @Mock
    private ReservaRepository reservaRepository;

    @InjectMocks
    private FacturaService facturaService;

    private Factura factura;

    @BeforeEach
    void setUp() {
        Persona titular = Persona.builder().id(1).build();
        Reserva reserva = Reserva.builder().id(9).persona(titular).build();
        factura = Factura.builder().id(FACTURA_ID).reserva(reserva).build();
    }

    private User usuario(int personaId, Role rol) {
        return User.builder().persona(Persona.builder().id(personaId).build()).role(rol).build();
    }

    @Test
    @DisplayName("Descargar factura: el titular de la reserva puede verla")
    void titularPuedeVerSuFactura() {
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        assertSame(factura, facturaService.obtenerFacturaParaUsuario(FACTURA_ID, usuario(1, Role.ROLE_USUARIO)));
    }

    @Test
    @DisplayName("Descargar factura: otro cliente recibe 'no encontrada', como si no existiera")
    void otroClienteNoPuedeVerla() {
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        FacturaInvalidaException ex = assertThrows(FacturaInvalidaException.class,
                () -> facturaService.obtenerFacturaParaUsuario(FACTURA_ID, usuario(2, Role.ROLE_USUARIO)));
        assertEquals("Factura no encontrada. ID: " + FACTURA_ID, ex.getMessage());
    }

    @Test
    @DisplayName("Descargar factura: empleados y administradores pueden ver cualquiera")
    void personalPuedeVerCualquiera() {
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));

        assertSame(factura, facturaService.obtenerFacturaParaUsuario(FACTURA_ID, usuario(2, Role.ROLE_EMPLEADO)));
        assertSame(factura, facturaService.obtenerFacturaParaUsuario(FACTURA_ID, usuario(3, Role.ROLE_ADMIN)));
    }

    @Test
    @DisplayName("Descargar factura: un usuario sin persona asociada no puede verla")
    void usuarioSinPersonaNoPuedeVerla() {
        when(facturaRepository.findById(FACTURA_ID)).thenReturn(Optional.of(factura));
        User sinPersona = User.builder().role(Role.ROLE_INCOMPLETO).build();

        assertThrows(FacturaInvalidaException.class,
                () -> facturaService.obtenerFacturaParaUsuario(FACTURA_ID, sinPersona));
    }
}
