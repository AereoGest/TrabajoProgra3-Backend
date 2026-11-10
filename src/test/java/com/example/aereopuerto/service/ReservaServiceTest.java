package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.Exceptions.ReservaInvalidaException;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.dto.PasajeRequest;
import com.example.aereopuerto.dto.ReservaDTO;
import com.example.aereopuerto.dto.ReservaRequest;
import com.example.aereopuerto.model.*;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;
import com.example.aereopuerto.dto.CarritoDTO;
import com.example.aereopuerto.dto.PagoRequest;
import com.example.aereopuerto.dto.PagoResponse;

import java.time.LocalTime;
import java.util.Map;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;
    @Mock
    private PersonaRepository personaRepository;
    @Mock
    private PasajeRepository pasajeRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PoliticaDevolucionService politicaService;
    @Mock
    private AsientoService asientoService;
    @Mock
    private VueloRepository vueloRepository;
    @Mock
    private AsientoRepository asientoRepository;
    @Mock
    private EquipajeRepository equipajeRepository;
    @Mock
    private AsistenciaRepository asistenciaRepository;
    @Mock
    private MercadoPagoService mercadoPagoService;
    @Mock
    private FacturaRepository facturaRepository;
    @Mock
    private CarritoRepository carritoRepository;
    @Mock
    private CarritoService carritoService;
    @Mock
    private UsuarioAutenticadoService usuarioAutenticadoService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ReservaService reservaService;

    private Persona cliente;
    private Avion avion;
    private Vuelo vuelo;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(reservaService, "tasaImpuesto", new BigDecimal("0.21"));
        ReflectionTestUtils.setField(reservaService, "tasaServicio", new BigDecimal("0.05"));

        cliente = Persona.builder()
                .id(10)
                .nombre("Juan")
                .apellido("Pérez")
                .identificador(Identificador.DNI)
                .numeroIdentificador("35123456")
                .build();

        avion = Avion.builder()
                .id(1)
                .modelo("Boeing 737")
                .capacidadPasajeros(100)
                .build();

        vuelo = Vuelo.builder()
                .id(5)
                .precioVuelo(100000.0)
                .estado(estadoVuelo.PROGRAMADO)
                .avion(avion)
                .build();
    }

    @Test
    @DisplayName("crearReserva calcula el total, ocupa asiento, asocia pasajes y enriquece email del titular")
    void crearReserva_ConDatosValidos_CreaReservaYOcupaAsiento() {
        Asiento asiento = Asiento.builder()
                .id(20)
                .codigo("12A")
                .ocupado(false)
                .precioExtra(10000.0)
                .avion(avion)
                .build();

        PasajeRequest pasajeReq = new PasajeRequest();
        pasajeReq.setClasesVuelo(ClasesVuelo.ECONOMICA);
        pasajeReq.setAsientoId(20);
        pasajeReq.setEquipajeIds(List.of());
        pasajeReq.setNombre("Juan");
        pasajeReq.setApellido("Pérez");
        pasajeReq.setTipoDocumento(Identificador.DNI);
        pasajeReq.setDocumento("35123456");
        pasajeReq.setFechaNacimiento(LocalDate.of(1990, 5, 15));
        pasajeReq.setEmail("juan@aerogest.com");

        ReservaRequest request = new ReservaRequest();
        request.setPersonaId(10);
        request.setVueloId(5);
        request.setCantidadPasajes(1);
        request.setPasajes(List.of(pasajeReq));

        when(personaRepository.findById(10)).thenReturn(Optional.of(cliente));
        when(userRepository.findByPersonaId(10)).thenReturn(Optional.of(
                User.builder().id(1).email("juan@aerogest.com").isVerified(true).persona(cliente).build()
        ));
        when(vueloRepository.findById(5)).thenReturn(Optional.of(vuelo));
        when(pasajeRepository.countByVueloId(5)).thenReturn(0L);
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(false);
        when(asientoRepository.findById(20)).thenReturn(Optional.of(asiento));
        when(equipajeRepository.findAllById(any())).thenReturn(List.of());
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> {
            Reserva r = inv.getArgument(0);
            r.setId(99);
            return r;
        });

        Reserva resultado = reservaService.crearReserva(request);

        assertThat(resultado.getId()).isEqualTo(99);
        assertThat(resultado.getEstadoReserva()).isEqualTo(EstadoReserva.PROCESANDO);
        assertThat(resultado.getCantidadPasajes()).isEqualTo(1);
        assertThat(resultado.getPasajes()).hasSize(1);
        assertThat(resultado.getPersona().getEmail()).isEqualTo("juan@aerogest.com");
        // base 100000 + extras 10000 + impuestos (21000) = 131000; + 5% servicio (6550) = 137550
        assertThat(resultado.getValor()).isEqualByComparingTo("137550");
        assertThat(asiento.getOcupado()).isTrue();
        verify(asientoRepository).save(asiento);
    }

    @Test
    @DisplayName("crearReserva rechaza vuelo cancelado")
    void crearReserva_VueloCancelado_LanzaReservaInvalidaException() {
        vuelo.setEstado(estadoVuelo.CANCELADO);

        ReservaRequest request = new ReservaRequest();
        request.setPersonaId(10);
        request.setVueloId(5);
        request.setCantidadPasajes(1);
        request.setPasajes(List.of());

        when(personaRepository.findById(10)).thenReturn(Optional.of(cliente));
        when(userRepository.findByPersonaId(10)).thenReturn(Optional.empty());
        when(vueloRepository.findById(5)).thenReturn(Optional.of(vuelo));

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessageContaining("está cancelado");
    }

    @Test
    @DisplayName("crearReserva rechaza asiento ya ocupado")
    void crearReserva_AsientoOcupado_LanzaReservaInvalidaException() {
        Asiento asientoOcupado = Asiento.builder()
                .id(20)
                .codigo("12A")
                .ocupado(true)
                .avion(avion)
                .build();

        PasajeRequest pasajeReq = new PasajeRequest();
        pasajeReq.setClasesVuelo(ClasesVuelo.ECONOMICA);
        pasajeReq.setAsientoId(20);
        pasajeReq.setNombre("Juan");
        pasajeReq.setApellido("Pérez");
        pasajeReq.setTipoDocumento(Identificador.DNI);
        pasajeReq.setDocumento("35123456");
        pasajeReq.setFechaNacimiento(LocalDate.of(1990, 5, 15));

        ReservaRequest request = new ReservaRequest();
        request.setPersonaId(10);
        request.setVueloId(5);
        request.setCantidadPasajes(1);
        request.setPasajes(List.of(pasajeReq));

        when(personaRepository.findById(10)).thenReturn(Optional.of(cliente));
        when(userRepository.findByPersonaId(10)).thenReturn(Optional.empty());
        when(vueloRepository.findById(5)).thenReturn(Optional.of(vuelo));
        when(pasajeRepository.countByVueloId(5)).thenReturn(0L);
        when(reservaRepository.existsByCodigoReserva(anyString())).thenReturn(false);
        when(asientoRepository.findById(20)).thenReturn(Optional.of(asientoOcupado));

        assertThatThrownBy(() -> reservaService.crearReserva(request))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessageContaining("ya está ocupado");
    }

    @Test
    @DisplayName("actualizarReserva actualiza titular, estado y valor manteniendo la cantidad real de pasajes")
    void actualizarReserva_DatosValidos_ActualizaCamposCorrectamente() {
        Persona nuevoTitular = Persona.builder()
                .id(25)
                .nombre("María")
                .apellido("Gómez")
                .build();

        Pasaje pasaje = Pasaje.builder().id(1).nombre("Juan").apellido("Pérez").build();
        List<Pasaje> pasajes = new ArrayList<>(List.of(pasaje));

        Reserva existente = Reserva.builder()
                .id(50)
                .codigoReserva("AC-2026-ABCD")
                .persona(cliente)
                .estadoReserva(EstadoReserva.PROCESANDO)
                .valor(new BigDecimal("100000"))
                .cantidadPasajes(1)
                .fechaReserva(LocalDateTime.now().minusDays(1))
                .pasajes(pasajes)
                .build();

        ReservaDTO dto = new ReservaDTO();
        dto.setClienteId(25);
        dto.setEstadoReserva(EstadoReserva.CONFIRMADO);
        dto.setValor(125000.0);
        dto.setCantidadPasajes(99); // Debe sincronizarse con la cantidad real de pasajes (1)

        when(reservaRepository.findById(50)).thenReturn(Optional.of(existente));
        when(personaRepository.findById(25)).thenReturn(Optional.of(nuevoTitular));
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.findByPersonaId(25)).thenReturn(Optional.empty());

        Reserva actualizada = reservaService.actualizarReserva(50, dto);

        assertThat(actualizada.getPersona()).isEqualTo(nuevoTitular);
        assertThat(actualizada.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADO);
        assertThat(actualizada.getValor()).isEqualByComparingTo("125000.0");
        assertThat(actualizada.getCantidadPasajes()).isEqualTo(1);
    }

    @Test
    @DisplayName("actualizarReserva impide reactivar una reserva que ya está CANCELADA")
    void actualizarReserva_ReservaCancelada_ImpideReactivar() {
        Reserva cancelada = Reserva.builder()
                .id(50)
                .persona(cliente)
                .estadoReserva(EstadoReserva.CANCELADA)
                .valor(new BigDecimal("100000"))
                .cantidadPasajes(1)
                .pasajes(new ArrayList<>())
                .build();

        ReservaDTO dto = new ReservaDTO();
        dto.setClienteId(10);
        dto.setEstadoReserva(EstadoReserva.CONFIRMADO);
        dto.setValor(100000.0);

        when(reservaRepository.findById(50)).thenReturn(Optional.of(cancelada));
        when(personaRepository.findById(10)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() -> reservaService.actualizarReserva(50, dto))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessageContaining("No se puede reactivar una reserva que ya fue cancelada");
    }

    @Test
    @DisplayName("cancelarReserva realiza baja lógica: pasa a CANCELADA, libera asientos y conserva pasajes")
    void cancelarReserva_BajaLogica_LiberaAsientosYConservaPasajes() {
        Asiento asiento = Asiento.builder()
                .id(30)
                .codigo("4B")
                .ocupado(true)
                .avion(avion)
                .build();

        Pasaje pasaje = Pasaje.builder()
                .id(7)
                .codigoPasaje("PAS12345")
                .asiento(asiento)
                .nombre("Juan")
                .apellido("Pérez")
                .build();

        List<Pasaje> pasajes = new ArrayList<>(List.of(pasaje));
        Reserva reserva = Reserva.builder()
                .id(60)
                .codigoReserva("AC-2026-XYZW")
                .persona(cliente)
                .estadoReserva(EstadoReserva.CONFIRMADO)
                .valor(new BigDecimal("150000"))
                .cantidadPasajes(1)
                .pasajes(pasajes)
                .build();

        when(reservaRepository.findById(60)).thenReturn(Optional.of(reserva));
        when(politicaService.calcular(reserva)).thenReturn(new BigDecimal("150000"));

        reservaService.cancelarReserva(60);

        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(asiento.getOcupado()).isFalse();
        assertThat(pasaje.getAsiento()).isNull();
        assertThat(reserva.getPasajes()).hasSize(1);
        assertThat(reserva.getMontoReembolso()).isEqualByComparingTo("150000");
        assertThat(reserva.getMotivoCancelacion()).isEqualTo(ReservaService.MOTIVO_CANCELADA_POR_AEROLINEA);
        verify(asientoRepository).save(asiento);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Si cancela el propio cliente, se guarda el reembolso de la política y el motivo del cliente")
    void cancelarReservaPorUsuario_cliente_guardaReembolsoYMotivo() {
        Reserva reserva = Reserva.builder()
                .id(61)
                .codigoReserva("AC-2026-ABCD")
                .persona(cliente)
                .estadoReserva(EstadoReserva.CONFIRMADO)
                .valor(new BigDecimal("200000"))
                .cantidadPasajes(0)
                .pasajes(new ArrayList<>())
                .build();
        User titular = User.builder().id(1).email("juan@aerogest.com").role(Role.ROLE_USUARIO).persona(cliente).build();

        when(reservaRepository.findById(61)).thenReturn(Optional.of(reserva));
        when(politicaService.calcular(reserva)).thenReturn(new BigDecimal("30000.00"));

        reservaService.cancelarReservaPorUsuario(61, titular);

        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(reserva.getMontoReembolso()).isEqualByComparingTo("30000");
        assertThat(reserva.getMotivoCancelacion()).isEqualTo(ReservaService.MOTIVO_CANCELADA_POR_CLIENTE);
    }

    private Reserva reservaPendienteDePago() {
        vuelo.setAeropuertoOrigen(Aeropuerto.builder().ciudad("Buenos Aires").codigoIata("EZE").build());
        vuelo.setAeropuertoDestino(Aeropuerto.builder().ciudad("Madrid").codigoIata("MAD").build());
        vuelo.setFechaSalida(LocalDate.of(2026, 12, 15));
        vuelo.setHoraSalida(LocalTime.of(10, 5));
        Reserva reserva = Reserva.builder()
                .id(70)
                .codigoReserva("AC-2026-PAGO")
                .persona(cliente)
                .estadoReserva(EstadoReserva.PROCESANDO)
                .valor(new BigDecimal("150000"))
                .pasajes(new ArrayList<>())
                .build();
        reserva.agregarPasaje(Pasaje.builder()
                .nombre("Juan").apellido("Pérez")
                .clasesVuelo(ClasesVuelo.ECONOMICA)
                .vuelo(vuelo)
                .asiento(Asiento.builder().codigo("12A").build())
                .build());
        reserva.agregarPasaje(Pasaje.builder()
                .nombre("Ana").apellido("Gómez")
                .clasesVuelo(ClasesVuelo.ECONOMICA)
                .vuelo(vuelo)
                .build());
        return reserva;
    }

    private PagoRequest pagoConVisa() {
        PagoRequest pago = new PagoRequest();
        pago.setToken("tok");
        pago.setInstallments(3);
        pago.setPaymentMethodId("visa");
        pago.setPayerEmail("pagador@mail.com");
        pago.setPayerDocType("DNI");
        pago.setPayerDocNumber("35123456");
        pago.setCuil("20-35123456-7");
        pago.setSituacionFiscal("Consumidor final");
        return pago;
    }

    @Test
    @DisplayName("Si el pago se aprueba, publica el recibo con el mail de la cuenta y los datos de la compra")
    void pagarReserva_aprobado_publicaRecibo() {
        Reserva reserva = reservaPendienteDePago();
        User titular = User.builder().id(1).email("juan@aerogest.com").persona(cliente).build();
        CarritoDTO carrito = new CarritoDTO();
        carrito.setId(3);

        when(reservaRepository.findById(70)).thenReturn(Optional.of(reserva));
        when(mercadoPagoService.procesarPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("status", "approved", "id", 999L));
        when(carritoService.getCarritoByPersonaId(10)).thenReturn(carrito);

        PagoResponse respuesta = reservaService.pagarReserva(70, pagoConVisa(), titular);

        assertThat(respuesta.estadoReserva()).isEqualTo(EstadoReserva.CONFIRMADO);
        ArgumentCaptor<CompraConfirmadaEvent> captor = ArgumentCaptor.forClass(CompraConfirmadaEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        CompraConfirmadaEvent recibo = captor.getValue();
        assertThat(recibo.email()).isEqualTo("juan@aerogest.com");
        assertThat(recibo.nombre()).isEqualTo("Juan");
        assertThat(recibo.codigoReserva()).isEqualTo("AC-2026-PAGO");
        assertThat(recibo.descripcionVuelo()).isEqualTo("Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:05");
        assertThat(recibo.pasajeros()).containsExactly(
                new CompraConfirmadaEvent.Pasajero("Juan Pérez", "Económica", "12A"),
                new CompraConfirmadaEvent.Pasajero("Ana Gómez", "Económica", null));
        assertThat(recibo.total()).isEqualByComparingTo("150000");
        assertThat(recibo.cuotas()).isEqualTo(3);
        assertThat(recibo.medioDePago()).isEqualTo("Tarjeta de crédito");
    }

    @Test
    @DisplayName("Si el pago se rechaza, no se manda recibo")
    void pagarReserva_rechazado_noPublicaRecibo() {
        Reserva reserva = reservaPendienteDePago();
        User titular = User.builder().id(1).email("juan@aerogest.com").persona(cliente).build();

        when(reservaRepository.findById(70)).thenReturn(Optional.of(reserva));
        when(mercadoPagoService.procesarPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("status", "rejected", "id", 999L));

        reservaService.pagarReserva(70, pagoConVisa(), titular);

        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        verifyNoInteractions(eventPublisher);
    }

    @Test
    @DisplayName("cancelarReserva rechaza cancelar una reserva que ya está CANCELADA")
    void cancelarReserva_YaCancelada_LanzaReservaInvalidaException() {
        Reserva reserva = Reserva.builder()
                .id(60)
                .estadoReserva(EstadoReserva.CANCELADA)
                .build();

        when(reservaRepository.findById(60)).thenReturn(Optional.of(reserva));

        assertThatThrownBy(() -> reservaService.cancelarReserva(60))
                .isInstanceOf(ReservaInvalidaException.class)
                .hasMessageContaining("ya se encuentra cancelada");
    }
}
