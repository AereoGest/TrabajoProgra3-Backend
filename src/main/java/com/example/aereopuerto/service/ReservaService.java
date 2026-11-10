package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.PagoException;
import com.example.aereopuerto.Exceptions.PersonaInvalidaException;
import com.example.aereopuerto.Exceptions.ReservaInvalidaException;
import com.example.aereopuerto.Specifications.ReservaSpecification;
import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.auth.repository.UserRepository;
import com.example.aereopuerto.auth.service.UsuarioAutenticadoService;
import com.example.aereopuerto.dto.*;
import com.example.aereopuerto.model.*;
import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.MetodosDePago;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ReservaService {

    // Motivos que se guardan en la reserva al cancelarla (el front los traduce)
    public static final String MOTIVO_CANCELADA_POR_CLIENTE = "Cancelada por el cliente";
    public static final String MOTIVO_CANCELADA_POR_AEROLINEA = "Cancelada por la aerolínea";

    @Autowired
    private final ReservaRepository reservaRepository;
    private final PersonaRepository personaRepository;
    private final PasajeRepository pasajeRepository;
    private final UserRepository userRepository;
    private final PoliticaDevolucionService politicaService;
    private final AsientoService asientoService;
    private final VueloRepository vueloRepository;
    private final AsientoRepository asientoRepository;
    private final EquipajeRepository equipajeRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final MercadoPagoService mercadoPagoService;
    private final FacturaRepository facturaRepository;
    private final CarritoRepository carritoRepository;
    private final CarritoService carritoService;
    private final UsuarioAutenticadoService usuarioAutenticadoService;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${tarifas.impuesto}")
    private BigDecimal tasaImpuesto;

    @Value("${tarifas.servicio}")
    private BigDecimal tasaServicio;

    @Cacheable(value = "reservas", key = "#id")
    public Reserva obtenerReservaPorId(Integer id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaInvalidaException("Reserva no encontrada. ID: " + id));
        enriquecerEmailPersona(reserva);
        return reserva;
    }

    @Cacheable(value = "reservas", key = "'todos'")
    public List<Reserva> obtenerTodasLasReservas() {
        List<Reserva> reservas = reservaRepository.findAll();
        for (Reserva r : reservas) {
            enriquecerEmailPersona(r);
        }
        return reservas;
    }

    @Cacheable(value = "reservas", key = "'persona_' + #usuarioAutenticado.persona.id")
    public List<Reserva> obtenerMisReservas(User usuarioAutenticado) {
        Persona persona = usuarioAutenticado.getPersona();
        return reservaRepository.findByPersona(persona);
    }

    @Transactional
    @Caching(
            put = @CachePut(value = "reservas", key = "#result.id"),
            evict = {
                    @CacheEvict(value = "reservas", key = "'todos'"),
                    @CacheEvict(value = "reservas", key = "'persona_' + #request.personaId")
            })
    public Reserva crearReserva(ReservaRequest request) {
        if (request.getPersonaId() == null) {
            throw new PersonaInvalidaException("El id de la persona es obligatorio");
        }
        Persona persona = personaRepository.findById(request.getPersonaId())
                .orElseThrow(() -> new PersonaInvalidaException(
                        "Usuario no encontrado. ID: " + request.getPersonaId()));

        userRepository.findByPersonaId(persona.getId())
                .ifPresent(u -> usuarioAutenticadoService.validarCuentaVerificada(u, "realizar una reserva"));

        Vuelo vuelo = vueloRepository.findById(request.getVueloId())
                .orElseThrow(() -> new ReservaInvalidaException(
                        "Vuelo no encontrado. ID: " + request.getVueloId()));
        if (vuelo.getEstado() == estadoVuelo.CANCELADO) {
            throw new ReservaInvalidaException("El vuelo " + vuelo.getId() + " está cancelado.");
        }
        long vendidos = pasajeRepository.countByVueloId(vuelo.getId());
        if (vendidos + request.getCantidadPasajes() > vuelo.getAvion().getCapacidadPasajeros()) {
            throw new ReservaInvalidaException("Sin disponibilidad en el vuelo " + vuelo.getId());
        }
        validarPasajeros(request);

        Reserva reserva = Reserva.builder()
                .persona(persona)
                .codigoReserva(generarCodigoReserva())
                .estadoReserva(EstadoReserva.PROCESANDO)
                .fechaReserva(LocalDateTime.now())
                .valor(BigDecimal.ZERO)
                .cantidadPasajes(0)
                .build();

        BigDecimal base = BigDecimal.ZERO;
        BigDecimal extras = BigDecimal.ZERO;

        for (int i = 0; i < request.getPasajes().size(); i++) {
            PasajeRequest p = request.getPasajes().get(i);
            int nro = i + 1;
            base = base.add(BigDecimal.valueOf(vuelo.getPrecioVuelo()));
            Asiento asiento = null;
            if (p.getAsientoId() != null) {
                asiento = asientoRepository.findById(p.getAsientoId())
                        .orElseThrow(() -> new ReservaInvalidaException(
                                "Pasajero " + nro + ": asiento no encontrado. ID: " + p.getAsientoId()));
                if (asiento.getAvion() == null || vuelo.getAvion() == null
                        || !Objects.equals(asiento.getAvion().getId(), vuelo.getAvion().getId())) {
                    throw new ReservaInvalidaException(
                            "Pasajero " + nro + ": el asiento " + asiento.getCodigo()
                                    + " no pertenece al avión de este vuelo");
                }
                if (Boolean.TRUE.equals(asiento.getOcupado())) {
                    throw new ReservaInvalidaException(
                            "Pasajero " + nro + ": el asiento " + asiento.getCodigo() + " ya está ocupado");
                }
                asiento.setOcupado(true);
                asientoRepository.save(asiento);
                extras = extras.add(BigDecimal.valueOf(asiento.getPrecioExtra() != null ? asiento.getPrecioExtra() : 0));
            }

            List<Integer> equipajeIds = p.getEquipajeIds() == null
                    ? List.of()
                    : p.getEquipajeIds().stream().distinct().toList();
            List<Equipaje> equipajes = new ArrayList<>(equipajeRepository.findAllById(equipajeIds));
            if (equipajes.size() != equipajeIds.size()) {
                throw new ReservaInvalidaException("Pasajero " + nro + ": alguno de los equipajes no existe");
            }
            for (Equipaje e : equipajes) {
                extras = extras.add(BigDecimal.valueOf(e.getPrecio() != null ? e.getPrecio() : 0));
            }

            AsistenciaAlViajero asistencia = null;
            if (p.getAsistenciaId() != null) {
                asistencia = asistenciaRepository.findById(p.getAsistenciaId())
                        .orElseThrow(() -> new ReservaInvalidaException(
                                "Pasajero " + nro + ": asistencia no encontrada. ID: " + p.getAsistenciaId()));
                extras = extras.add(BigDecimal.valueOf(asistencia.getPrecio()));
            }

            reserva.agregarPasaje(Pasaje.builder()
                    .codigoPasaje(UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .clasesVuelo(p.getClasesVuelo())
                    .vuelo(vuelo)
                    .asiento(asiento)
                    .equipajes(equipajes)
                    .asistenciaAlViajero(asistencia)
                    .nombre(p.getNombre())
                    .apellido(p.getApellido())
                    .tipoDocumento(p.getTipoDocumento())
                    .documento(p.getDocumento())
                    .fechaNacimiento(p.getFechaNacimiento())
                    .email(p.getEmail())
                    .build());
        }

        reserva.setValor(calcularTotal(base, extras));
        Reserva guardada = reservaRepository.save(reserva);
        enriquecerEmailPersona(guardada);
        return guardada;
    }

    @Transactional
    @CacheEvict(value = "reservas", allEntries = true)
    public PagoResponse pagarReserva(Integer id, PagoRequest pago, User usuarioAutenticado) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaInvalidaException("Reserva no encontrada. ID: " + id));

        if (!reserva.getPersona().getId().equals(usuarioAutenticado.getPersona().getId())) {
            throw new ReservaInvalidaException("La reserva no pertenece al usuario autenticado.");
        }
        if (reserva.getEstadoReserva() != EstadoReserva.PROCESANDO) {
            throw new ReservaInvalidaException(
                    "La reserva no está pendiente de pago. Estado: " + reserva.getEstadoReserva());
        }

        // se valida ANTES de cobrar
        String cuil = pago.getCuil().replaceAll("[-\\s]", "");
        if (!cuil.matches("\\d{11}")) {
            throw new ReservaInvalidaException("El CUIL/CUIT debe tener 11 dígitos");
        }

        Map<String, Object> pagoMp = mercadoPagoService.procesarPago(
                pago.getToken(),
                reserva.getValor().doubleValue(),
                pago.getInstallments(),
                pago.getPaymentMethodId(),
                pago.getIssuerId(),
                pago.getPayerEmail(),
                pago.getPayerDocType(),
                pago.getPayerDocNumber(),
                "Reserva " + reserva.getCodigoReserva(),
                reserva.getCodigoReserva());

        String status = String.valueOf(pagoMp.get("status"));
        if ("error".equals(status)) {
            // no se sabe si se cobró: la reserva queda PROCESANDO y se puede reintentar
            throw new PagoException(String.valueOf(pagoMp.getOrDefault("error", "No se pudo procesar el pago")));
        }

        String detalle = pagoMp.get("status_detail") != null ? pagoMp.get("status_detail").toString() : null;
        reserva.setMercadoPagoId(pagoMp.get("id") instanceof Number n ? n.longValue() : null);

        switch (status) {
            case "approved" -> {
                reserva.setEstadoReserva(EstadoReserva.CONFIRMADO);
                reservaRepository.save(reserva);
                Factura factura = crearFactura(reserva, pago, cuil);
                Integer personaId = reserva.getPersona().getId();
                carritoService.clearCarrito(personaId, carritoService.getCarritoByPersonaId(personaId).getId());
                // El recibo sale recién cuando se confirma la transacción
                eventPublisher.publishEvent(armarRecibo(reserva, factura, usuarioAutenticado.getEmail()));
            }
            case "rejected" -> liberarAsientosYCancelar(reserva);
            default -> reservaRepository.save(reserva); // in_process / pending: queda PROCESANDO
        }

        return new PagoResponse(status, detalle, reserva.getCodigoReserva(), reserva.getEstadoReserva());
    }

    private void liberarAsientosYCancelar(Reserva reserva) {
        for (Pasaje p : reserva.getPasajes()) {
            if (p.getAsiento() != null) {
                p.getAsiento().setOcupado(false);
                asientoRepository.save(p.getAsiento());
                p.setAsiento(null);
            }
        }
        reserva.setEstadoReserva(EstadoReserva.CANCELADA);
        reservaRepository.save(reserva);
    }

    // ---------- helpers de crearReserva ----------
    private BigDecimal calcularTotal(BigDecimal base, BigDecimal extras) {
        BigDecimal impuestos = base.multiply(tasaImpuesto).setScale(0, RoundingMode.HALF_UP);
        BigDecimal subtotal = base.add(extras).add(impuestos);
        BigDecimal servicio = subtotal.multiply(tasaServicio).setScale(0, RoundingMode.HALF_UP);
        return subtotal.add(servicio);
    }

    private void validarPasajeros(ReservaRequest request) {
        List<PasajeRequest> pasajes = request.getPasajes();

        if (pasajes.size() != request.getCantidadPasajes()) {
            throw new ReservaInvalidaException(
                    "Se compraron " + request.getCantidadPasajes() + " pasajes pero se cargaron los datos de "
                            + pasajes.size() + ". Completá los datos de cada pasajero.");
        }

        Set<String> documentos = new HashSet<>();
        Set<Integer> asientos = new HashSet<>();

        for (int i = 0; i < pasajes.size(); i++) {
            PasajeRequest p = pasajes.get(i);
            int nro = i + 1;

            normalizar(p);

            boolean documentoValido = switch (p.getTipoDocumento()) {
                case DNI -> p.getDocumento().matches("\\d{7,8}");
                case PASAPORTE -> p.getDocumento().matches("[A-Z0-9]{6,20}");
            };
            if (!documentoValido) {
                throw new ReservaInvalidaException(
                        "Pasajero " + nro + ": el documento no es válido para el tipo " + p.getTipoDocumento());
            }

            if (!documentos.add(p.getTipoDocumento() + "-" + p.getDocumento())) {
                throw new ReservaInvalidaException(
                        "Pasajero " + nro + ": el documento " + p.getDocumento() + " está repetido en la reserva");
            }

            if (p.getAsientoId() != null && !asientos.add(p.getAsientoId())) {
                throw new ReservaInvalidaException(
                        "Pasajero " + nro + ": el asiento está asignado a más de un pasajero");
            }
        }
    }

    private void normalizar(PasajeRequest p) {
        if (p.getDocumento() == null || p.getDocumento().isBlank() || p.getTipoDocumento() == null) {
            throw new ReservaInvalidaException("El tipo y número de documento son obligatorios");
        }
        p.setNombre(p.getNombre() != null ? p.getNombre().trim() : "");
        p.setApellido(p.getApellido() != null ? p.getApellido().trim() : "");
        p.setDocumento(p.getDocumento().replaceAll("[\\s.\\-]", "").toUpperCase());
        p.setEmail(p.getEmail() == null || p.getEmail().isBlank() ? null : p.getEmail().trim());
    }

    private static final String ALFABETO_CODIGO = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private String generarCodigoReserva() {
        String codigo;
        do {
            StringBuilder sufijo = new StringBuilder();
            for (int i = 0; i < 4; i++) {
                sufijo.append(ALFABETO_CODIGO.charAt(RANDOM.nextInt(ALFABETO_CODIGO.length())));
            }
            codigo = "AC-" + Year.now().getValue() + "-" + sufijo;
        } while (reservaRepository.existsByCodigoReserva(codigo));
        return codigo;
    }

    @Transactional
    @CacheEvict(value = "reservas", allEntries = true)
    public Reserva actualizarReserva(Integer id, ReservaDTO reservaDTO) {
        if (reservaDTO == null) {
            throw new ReservaInvalidaException("Los datos de la reserva son obligatorios");
        }

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaInvalidaException(
                        "Reserva no encontrada. ID: " + id));

        if (reservaDTO.getClienteId() == null) {
            throw new PersonaInvalidaException("El titular de la reserva es obligatorio");
        }
        Persona persona = personaRepository.findById(reservaDTO.getClienteId())
                .orElseThrow(() -> new PersonaInvalidaException(
                        "Usuario no encontrado. ID: " + reservaDTO.getClienteId()));

        if (reservaDTO.getValor() == null || reservaDTO.getValor() < 0) {
            throw new ReservaInvalidaException("El valor de la reserva debe ser mayor o igual a cero");
        }
        if (reservaDTO.getEstadoReserva() == null) {
            throw new ReservaInvalidaException("El estado de la reserva es obligatorio");
        }

        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA
                && reservaDTO.getEstadoReserva() != EstadoReserva.CANCELADA) {
            throw new ReservaInvalidaException(
                    "No se puede reactivar una reserva que ya fue cancelada");
        }

        reserva.setPersona(persona);
        reserva.setValor(BigDecimal.valueOf(reservaDTO.getValor()));

        if (reservaDTO.getFechaReserva() != null) {
            reserva.setFechaReserva(reservaDTO.getFechaReserva());
        }

        if (reservaDTO.getEstadoReserva() == EstadoReserva.CANCELADA
                && reserva.getEstadoReserva() != EstadoReserva.CANCELADA) {
            registrarReembolso(reserva, MOTIVO_CANCELADA_POR_AEROLINEA);
            liberarAsientosYCancelar(reserva);
        } else {
            reserva.setEstadoReserva(reservaDTO.getEstadoReserva());
            if (reserva.getPasajes() != null && !reserva.getPasajes().isEmpty()) {
                reserva.setCantidadPasajes(reserva.getPasajes().size());
            } else if (reservaDTO.getCantidadPasajes() != null && reservaDTO.getCantidadPasajes() >= 0) {
                reserva.setCantidadPasajes(reservaDTO.getCantidadPasajes());
            }
        }

        Reserva guardada = reservaRepository.save(reserva);
        enriquecerEmailPersona(guardada);
        return guardada;
    }

    private void enriquecerEmailPersona(Reserva r) {
        if (r != null && r.getPersona() != null && r.getPersona().getId() != null) {
            userRepository.findByPersonaId(r.getPersona().getId())
                    .ifPresent(u -> r.getPersona().setEmail(u.getEmail()));
        }
    }

    /** Cancelación hecha por un admin o empleado (ej. DELETE /api/reservas/{id}). */
    @Transactional
    @CacheEvict(value = "reservas", allEntries = true)
    public void cancelarReserva(Integer id) {
        cancelarReserva(id, MOTIVO_CANCELADA_POR_AEROLINEA);
    }

    /**
     * Cancela la reserva, libera sus asientos y guarda en la reserva el reembolso que corresponde
     * según la política de devolución (días hasta el vuelo) y el motivo, para que lo vean el cliente y el admin.
     */
    @Transactional
    @CacheEvict(value = "reservas", allEntries = true)
    public void cancelarReserva(Integer id, String motivo) {

        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaInvalidaException("Reserva no encontrada"));

        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new ReservaInvalidaException(
                    "La reserva ya se encuentra cancelada");
        }

        registrarReembolso(reserva, motivo);
        liberarAsientosYCancelar(reserva);
    }

    @Transactional
    @CacheEvict(value = "reservas", allEntries = true)
    public void cancelarReservaPorUsuario(Integer id, User usuarioAutenticado) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaInvalidaException("Reserva no encontrada"));
        boolean esAdminOEmpleado = usuarioAutenticado.getRole() == Role.ROLE_ADMIN
                || usuarioAutenticado.getRole() == Role.ROLE_EMPLEADO;
        if (!esAdminOEmpleado && !reserva.getPersona().getId().equals(usuarioAutenticado.getPersona().getId())) {
            throw new ReservaInvalidaException("La reserva no pertenece al usuario autenticado.");
        }
        cancelarReserva(id, esAdminOEmpleado ? MOTIVO_CANCELADA_POR_AEROLINEA : MOTIVO_CANCELADA_POR_CLIENTE);
    }

    /** Calcula el reembolso con la política de devolución y lo deja registrado en la reserva junto al motivo. */
    private void registrarReembolso(Reserva reserva, String motivo) {
        BigDecimal reembolso = politicaService.calcular(reserva);
        reserva.setMontoReembolso(reembolso != null ? reembolso : BigDecimal.ZERO);
        reserva.setMotivoCancelacion(motivo);
    }

    private Factura crearFactura(Reserva reserva, PagoRequest pago, String cuil) {
        Factura factura = Factura.builder()
                .reserva(reserva)
                .CUIL(cuil)
                .situacionFiscal(pago.getSituacionFiscal())
                .metodoDePago(mapPaymentMethod(pago.getPaymentMethodId()))
                .fechaEmision(LocalDateTime.now())
                .cuotas(pago.getInstallments())
                .totalPagado(reserva.getValor().doubleValue())
                .build();
        facturaRepository.save(factura);
        return factura;
    }

    /** Arma los datos del recibo de compra mientras la reserva y sus pasajes siguen cargados. */
    private CompraConfirmadaEvent armarRecibo(Reserva reserva, Factura factura, String email) {
        List<Pasaje> pasajes = reserva.getPasajes() != null ? reserva.getPasajes() : List.of();
        String descripcionVuelo = pasajes.stream()
                .map(Pasaje::getVuelo)
                .filter(Objects::nonNull)
                .findFirst()
                .map(DescripcionVuelo::de)
                .orElse(null);
        List<CompraConfirmadaEvent.Pasajero> pasajeros = pasajes.stream()
                .map(p -> new CompraConfirmadaEvent.Pasajero(
                        (p.getNombre() + " " + p.getApellido()).trim(),
                        describirClase(p.getClasesVuelo()),
                        p.getAsiento() != null ? p.getAsiento().getCodigo() : null))
                .toList();
        return new CompraConfirmadaEvent(
                email,
                reserva.getPersona() != null ? reserva.getPersona().getNombre() : null,
                reserva.getCodigoReserva(),
                descripcionVuelo,
                pasajeros,
                reserva.getValor(),
                factura.getCuotas(),
                describirMedioDePago(factura.getMetodoDePago()));
    }

    private static String describirClase(ClasesVuelo clase) {
        if (clase == null) return null;
        return switch (clase) {
            case TURISTA -> "Turista";
            case ECONOMICA -> "Económica";
            case BUSINESS -> "Business";
            case PRIMERA -> "Primera";
        };
    }

    private static String describirMedioDePago(MetodosDePago metodo) {
        if (metodo == null) return null;
        return switch (metodo) {
            case TARJETA_CREDITO -> "Tarjeta de crédito";
            case TARJETA_DEBITO -> "Tarjeta de débito";
            case TRANSFERENCIA -> "Transferencia";
            case MERCADOPAGO -> "Mercado Pago";
        };
    }

    private static final Map<String, MetodosDePago> METODOS_DE_PAGO = Map.of(
            "visa", MetodosDePago.TARJETA_CREDITO,
            "master", MetodosDePago.TARJETA_CREDITO,
            "amex", MetodosDePago.TARJETA_CREDITO,
            "naranja", MetodosDePago.TARJETA_CREDITO,
            "cabal", MetodosDePago.TARJETA_CREDITO,
            "debmaster", MetodosDePago.TARJETA_DEBITO,
            "debvisa", MetodosDePago.TARJETA_DEBITO,
            "debcabal", MetodosDePago.TARJETA_DEBITO,
            "maestro", MetodosDePago.TARJETA_DEBITO);

    private MetodosDePago mapPaymentMethod(String paymentMethodId) {
        if (paymentMethodId == null) return MetodosDePago.MERCADOPAGO;
        return METODOS_DE_PAGO.getOrDefault(paymentMethodId.toLowerCase(), MetodosDePago.MERCADOPAGO);
    }
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "reservas", key = "#id"),
            @CacheEvict(value = "reservas", key = "'todos'"),
            @CacheEvict(value = "reservas", allEntries = true)
    })
    public void eliminarReserva(Integer id) {
        cancelarReserva(id);
    }

    public List<Reserva> buscarTodasReservasConFiltros(
            String email,
            EstadoReserva estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Double valorMinimo,
            Double valorMaximo,
            Integer cantidadPasajes) {

        Integer personaId = null;
        if (email != null && !email.trim().isEmpty()) {
            personaId = usuarioAutenticadoService.getPersonaPorEmail(email).getId();
        }

        return buscarReservasConFiltros(personaId, estado, fechaDesde, fechaHasta,
                valorMinimo, valorMaximo, cantidadPasajes);
    }

    public List<Reserva> buscarMisReservasConFiltros(
            Persona persona,
            EstadoReserva estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Double valorMinimo,
            Double valorMaximo,
            Integer cantidadPasajes) {

        return buscarReservasConFiltros(persona.getId(), estado, fechaDesde, fechaHasta,
                valorMinimo, valorMaximo, cantidadPasajes);
    }

    private List<Reserva> buscarReservasConFiltros(
            Integer personaId,
            EstadoReserva estado,
            LocalDateTime fechaDesde,
            LocalDateTime fechaHasta,
            Double valorMinimo,
            Double valorMaximo,
            Integer cantidadPasajes) {

        Specification<Reserva> spec = Specification
                .where(ReservaSpecification.porPersonaId(personaId))
                .and(ReservaSpecification.porEstado(estado))
                .and(ReservaSpecification.porFechaDesde(fechaDesde))
                .and(ReservaSpecification.porFechaHasta(fechaHasta))
                .and(ReservaSpecification.porValorMinimo(valorMinimo))
                .and(ReservaSpecification.porValorMaximo(valorMaximo))
                .and(ReservaSpecification.porCantidadPasajes(cantidadPasajes));

        return reservaRepository.findAll(spec);
    }

    @CacheEvict(value = "reservas", key = "'persona_' + #personaId")
    public void invalidarCachePersona(Integer personaId) {
    }

    @CacheEvict(value = "reservas", allEntries = true)
    public void hacerCheckin(Integer id) {
        Reserva reserva = reservaRepository.findById(id)
                .orElseThrow(() -> new ReservaInvalidaException("Reserva no encontrada"));
        if (reserva.getEstadoReserva() == EstadoReserva.CANCELADA) {
            throw new IllegalArgumentException("No se puede hacer check-in de una reserva cancelada");
        }
        List<Pasaje> pasajes = pasajeRepository.findByReservaId(reserva.getId());
        for (Pasaje p : pasajes) {
            if (p.getAsiento() == null && p.getVuelo() != null && p.getVuelo().getAvion() != null) {
                Asiento asiento = asientoService.asignarEconomicoAleatorio(p.getVuelo().getAvion().getId());
                p.setAsiento(asiento);
                pasajeRepository.save(p);
            }
        }
        reserva.setEstadoReserva(EstadoReserva.ACTIVO);
        reservaRepository.save(reserva);
    }
}
