package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.PagoException;
import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.Exceptions.ReservaInvalidaException;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.auth.service.EmailService;
import com.example.aereopuerto.dto.CarritoDTO;
import com.example.aereopuerto.dto.PagoRequest;
import com.example.aereopuerto.dto.PagoResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

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
    private EmailService emailService;

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

    @Test
    @DisplayName("pagarReserva con pago aprobado confirma la reserva, crea factura y limpia carrito")
    void pagarReserva_PagoAprobado_ConfirmaReservaYCreaFactura() {
        User usuario = User.builder().id(1).persona(cliente).build();

        Reserva reserva = Reserva.builder()
                .id(70)
                .codigoReserva("AC-PAGO-001")
                .persona(cliente)
                .estadoReserva(EstadoReserva.PROCESANDO)
                .valor(new BigDecimal("125000.00"))
                .cantidadPasajes(1)
                .pasajes(new ArrayList<>())
                .build();

        PagoRequest pago = new PagoRequest();
        pago.setToken("tok_123456");
        pago.setInstallments(1);
        pago.setPaymentMethodId("visa");
        pago.setPayerEmail("juan@aerogest.com");
        pago.setPayerDocType("DNI");
        pago.setPayerDocNumber("35123456");
        pago.setCuil("20351234561");
        pago.setSituacionFiscal("Consumidor Final");

        when(reservaRepository.findById(70)).thenReturn(Optional.of(reserva));
        when(mercadoPagoService.procesarPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.Map.of("status", "approved", "status_detail", "accredited", "id", 999888777L));

        CarritoDTO carritoDTO = new CarritoDTO();
        carritoDTO.setId(10);
        when(carritoService.getCarritoByPersonaId(10)).thenReturn(carritoDTO);

        PagoResponse response = reservaService.pagarReserva(70, pago, usuario);

        assertThat(response.status()).isEqualTo("approved");
        assertThat(response.estadoReserva()).isEqualTo(EstadoReserva.CONFIRMADO);
        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.CONFIRMADO);
        assertThat(reserva.getMercadoPagoId()).isEqualTo(999888777L);
        verify(reservaRepository).save(reserva);
        verify(facturaRepository).save(any(Factura.class));
        verify(carritoService).clearCarrito(10, 10);
        verify(emailService).sendReservaConfirmadaEmail(anyString(), anyString(), eq("AC-PAGO-001"), anyString(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("pagarReserva con pago rechazado cancela la reserva y libera asientos")
    void pagarReserva_PagoRechazado_CancelaReserva() {
        User usuario = User.builder().id(1).persona(cliente).build();

        Asiento asiento = Asiento.builder().id(15).codigo("1A").ocupado(true).build();
        Pasaje pasaje = Pasaje.builder().id(2).asiento(asiento).build();

        Reserva reserva = Reserva.builder()
                .id(71)
                .codigoReserva("AC-PAGO-002")
                .persona(cliente)
                .estadoReserva(EstadoReserva.PROCESANDO)
                .valor(new BigDecimal("90000.00"))
                .cantidadPasajes(1)
                .pasajes(new ArrayList<>(List.of(pasaje)))
                .build();

        PagoRequest pago = new PagoRequest();
        pago.setToken("tok_rejected");
        pago.setInstallments(1);
        pago.setPaymentMethodId("visa");
        pago.setPayerEmail("juan@aerogest.com");
        pago.setPayerDocType("DNI");
        pago.setPayerDocNumber("35123456");
        pago.setCuil("20351234561");
        pago.setSituacionFiscal("Consumidor Final");

        when(reservaRepository.findById(71)).thenReturn(Optional.of(reserva));
        when(mercadoPagoService.procesarPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.Map.of("status", "rejected", "status_detail", "cc_rejected_insufficient_amount"));

        PagoResponse response = reservaService.pagarReserva(71, pago, usuario);

        assertThat(response.status()).isEqualTo("rejected");
        assertThat(response.estadoReserva()).isEqualTo(EstadoReserva.CANCELADA);
        assertThat(asiento.getOcupado()).isFalse();
        assertThat(pasaje.getAsiento()).isNull();
        verify(asientoRepository).save(asiento);
        verify(reservaRepository).save(reserva);
        verifyNoInteractions(facturaRepository);
    }

    @Test
    @DisplayName("pagarReserva con error de Mercado Pago lanza PagoException y no cancela la reserva")
    void pagarReserva_ErrorMercadoPago_LanzaPagoException() {
        User usuario = User.builder().id(1).persona(cliente).build();

        Reserva reserva = Reserva.builder()
                .id(72)
                .codigoReserva("AC-PAGO-003")
                .persona(cliente)
                .estadoReserva(EstadoReserva.PROCESANDO)
                .valor(new BigDecimal("90000.00"))
                .cantidadPasajes(1)
                .pasajes(new ArrayList<>())
                .build();

        PagoRequest pago = new PagoRequest();
        pago.setToken("tok_error");
        pago.setInstallments(1);
        pago.setPaymentMethodId("visa");
        pago.setPayerEmail("juan@aerogest.com");
        pago.setPayerDocType("DNI");
        pago.setPayerDocNumber("35123456");
        pago.setCuil("20351234561");
        pago.setSituacionFiscal("Consumidor Final");

        when(reservaRepository.findById(72)).thenReturn(Optional.of(reserva));
        when(mercadoPagoService.procesarPago(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(java.util.Map.of("status", "error", "error", "No se pudo comunicar con Mercado Pago"));

        assertThatThrownBy(() -> reservaService.pagarReserva(72, pago, usuario))
                .isInstanceOf(PagoException.class)
                .hasMessageContaining("No se pudo comunicar con Mercado Pago");

        assertThat(reserva.getEstadoReserva()).isEqualTo(EstadoReserva.PROCESANDO);
    }
}
