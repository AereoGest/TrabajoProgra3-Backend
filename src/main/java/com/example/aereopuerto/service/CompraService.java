package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.*;
import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.dto.CompraDTO;
import com.example.aereopuerto.model.*;
import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.estadoVuelo;
import com.example.aereopuerto.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.example.aereopuerto.service.ReservaService;
import org.springframework.beans.factory.annotation.Value;

@Service
@RequiredArgsConstructor
@Transactional
public class CompraService {

    private final CarritoRepository carritoRepository;
    private final PasajeRepository pasajeRepository;
    private final ReservaRepository reservaRepository;
    private final FacturaRepository facturaRepository;
    private final EquipajeRepository equipajeRepository;
    private final AsistenciaRepository asistenciaRepository;
    private final AsientoRepository asientoRepository;
    private final AsientoService asientoService;
    private final ReservaService reservaService;

    @Value("${tarifas.impuesto:0.15}")
    private Double tasaImpuesto;

    public String confirmarCompra(CompraDTO dto, User usuarioAutenticado) {
        if (usuarioAutenticado != null && !Boolean.TRUE.equals(usuarioAutenticado.getIsVerified())) {
            throw new IllegalArgumentException("Debes verificar tu cuenta de correo electrónico antes de realizar compras.");
        }
        Carrito carrito = obtenerCarrito(usuarioAutenticado.getPersona().getId());
        validarCarrito(carrito);
        validarDisponibilidad(carrito);
        validarAsientos(carrito, dto.getAsientosSeleccionados());
        List<Equipaje> equipajes = new ArrayList<>();
        if (dto.getEquipajeId() != null) {
            for (Integer id : dto.getEquipajeId()) {
                equipajes.add(equipajeRepository.findById(id)
                        .orElseThrow(() -> new EquipajeInvalidoException("Equipaje no encontrado")));
            }
        }
        AsistenciaAlViajero asistencia = obtenerAsistencia(dto.getAsistenciaId());
        Double asientoExtra = dto.getAsientoExtra() != null ? dto.getAsientoExtra() : 0;
        Double servicioExtra = dto.getServicioExtra() != null ? dto.getServicioExtra() : 0;
        Reserva reserva = crearReserva(carrito, equipajes, asistencia, asientoExtra, servicioExtra);
        List<Pasaje> pasajes = generarPasajes(carrito, reserva, equipajes, asistencia, dto.getAsientosSeleccionados());
        guardarPasajes(pasajes);
        crearFactura(dto, reserva);
        vaciarCarrito(carrito);
        reservaService.invalidarCachePersona(carrito.getPersona().getId());
        return "AC-" + reserva.getId();
    }

    private Carrito obtenerCarrito(Integer personaId) {
        return carritoRepository.findByPersonaId(personaId)
                .orElseThrow(() -> new CarritoInvalidoException("Carrito no encontrado"));
    }

    private void validarAsientos(
            Carrito carrito,
            List<Integer> asientosIds) {

        if (asientosIds == null || asientosIds.isEmpty()) {
            return;
        }

        int cantidadPasajes = carrito.getItems()
                .stream()
                .mapToInt(CarritoItem::getCantidad)
                .sum();

        if (asientosIds.size() > cantidadPasajes) {
            throw new AsientoInvalidoException(
                    "No puede seleccionar más asientos que pasajes");
        }

        for (Integer id : asientosIds) {

            Asiento asiento = asientoRepository.findById(id)
                    .orElseThrow(() -> new AsientoInvalidoException("Asiento inexistente"));

            if (Boolean.TRUE.equals(asiento.getOcupado())) {
                throw new AsientoInvalidoException("Asiento ocupado");
            }
        }
    }

    private void validarCarrito(Carrito carrito) {
        if (carrito.getItems() == null || carrito.getItems().isEmpty()) {
            throw new CarritoInvalidoException("El carrito está vacío");
        }
    }

    private void validarDisponibilidad(Carrito carrito) {

        for (CarritoItem item : carrito.getItems()) {
            Vuelo vuelo = item.getVuelo();
            if (vuelo.getEstado() == estadoVuelo.CANCELADO) {
                throw new VueloInvalidoException("El vuelo " + vuelo.getId() + " está cancelado.");
            }
            int capacidad = vuelo.getAvion().getCapacidadPasajeros();
            long vendidos = pasajeRepository.countByVueloId(vuelo.getId());
            if (vendidos + item.getCantidad() > capacidad) {
                throw new VueloInvalidoException("Sin disponibilidad en vuelo " + vuelo.getId());
            }
        }
    }

    /*
     * private Equipaje obtenerEquipaje(Integer equipajeId) {
     * 
     * if (equipajeId == null) return null;
     * return equipajeRepository.findById(equipajeId)
     * .orElseThrow(() -> new EquipajeInvalidoException("Equipaje no encontrado"));
     * }
     */

    private AsistenciaAlViajero obtenerAsistencia(Integer asistenciaId) {
        if (asistenciaId == null)
            return null;
        return asistenciaRepository.findById(asistenciaId)
                .orElseThrow(() -> new AsistenciaInvalidaException("Asistencia no encontrada"));
    }

    private Reserva crearReserva(Carrito carrito, List<Equipaje> equipajes, AsistenciaAlViajero asistencia,
            Double asientoExtra, Double servicioExtra) {

        int totalPasajes = carrito.getItems().stream()
                .mapToInt(CarritoItem::getCantidad).sum();

        Double totalVuelo = carrito.getItems().stream()
                .mapToDouble(i -> i.getVuelo().getPrecioVuelo() * i.getCantidad()).sum();

        Double totalEquipaje = equipajes.stream()
                .mapToDouble(Equipaje::getPrecio).sum();

        Double totalAsistencia = asistencia != null ? asistencia.getPrecio() : 0;

        Double impuestos = totalVuelo * tasaImpuesto;
        Double total = totalVuelo + totalEquipaje + totalAsistencia + asientoExtra + servicioExtra + impuestos;

        Reserva reserva = Reserva.builder()
                .persona(carrito.getPersona())
                .cantidadPasajes(totalPasajes)
                .valor(BigDecimal.valueOf(total))
                .estadoReserva(EstadoReserva.CONFIRMADO)
                .fechaReserva(LocalDateTime.now())
                .build();

        return reservaRepository.save(reserva);
    }

    private List<Pasaje> generarPasajes(
            Carrito carrito,
            Reserva reserva,
            List<Equipaje> equipajes,
            AsistenciaAlViajero asistencia,
            List<Integer> asientosSeleccionados) {

        List<Pasaje> pasajes = new ArrayList<>();

        int indiceAsiento = 0;
        boolean sinAsientos = asientosSeleccionados == null || asientosSeleccionados.isEmpty();

        for (CarritoItem item : carrito.getItems()) {

            for (int i = 0; i < item.getCantidad(); i++) {

                Asiento asiento = null;

                if (sinAsientos) {
                    asiento = asientoService.asignarEconomicoAleatorio(item.getVuelo().getAvion().getId());
                } else if (indiceAsiento < asientosSeleccionados.size()) {

                    asiento = asientoRepository
                            .findById(asientosSeleccionados.get(indiceAsiento))
                            .orElseThrow(() -> new AsientoInvalidoException("Asiento no encontrado"));

                    if (Boolean.TRUE.equals(asiento.getOcupado())) {
                        asiento = asientoService.asignarEconomicoAleatorio(item.getVuelo().getAvion().getId());
                    } else {
                        asiento.setOcupado(true);
                    }

                    indiceAsiento++;
                }

                if (asiento != null) {
                    asientoRepository.save(asiento);
                }

                Pasaje p = Pasaje.builder()
                        .codigoPasaje(UUID.randomUUID().toString())
                        .clasesVuelo(item.getClaseVuelo())
                        .vuelo(item.getVuelo())
                        .reserva(reserva)
                        .equipajes(equipajes)
                        .asistenciaAlViajero(asistencia)
                        .asiento(asiento)
                        .build();

                pasajes.add(p);
            }
        }

        return pasajes;
    }

    private void guardarPasajes(List<Pasaje> pasajes) {
        pasajeRepository.saveAll(pasajes);
    }

    private void crearFactura(CompraDTO dto, Reserva reserva) {

        Factura factura = Factura.builder()
                .reserva(reserva)
                .CUIL(dto.getCuil())
                .situacionFiscal(dto.getSituacionFiscal())
                .metodoDePago(dto.getMetodoPago())
                .fechaEmision(LocalDateTime.now())
                .cuotas(dto.getCuotas())
                .totalPagado(reserva.getValor() != null ? reserva.getValor().doubleValue() : 0)
                .build();

        facturaRepository.save(factura);
    }

    private void vaciarCarrito(Carrito carrito) {
        carrito.getItems().clear();
        carritoRepository.save(carrito);
    }

    public Double calcularTotalCarrito(Integer personaId) {
        Carrito carrito = obtenerCarrito(personaId);
        return carrito.getItems().stream()
                .mapToDouble(i -> i.getVuelo().getPrecioVuelo() * i.getCantidad())
                .sum();
    }

    public void confirmarPagoMP(String externalReference, String paymentId) {
        // aca dsp agregamos logica cuando solucionemos problema credenciales
    }
}
