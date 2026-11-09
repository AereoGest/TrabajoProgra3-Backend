package com.example.aereopuerto.service;

import com.example.aereopuerto.Exceptions.FacturaInvalidaException;
import com.example.aereopuerto.dto.FacturaDTO;
import com.example.aereopuerto.model.Factura;
import com.example.aereopuerto.model.Persona;
import com.example.aereopuerto.model.Reserva;
import com.example.aereopuerto.repository.FacturaRepository;
import com.example.aereopuerto.repository.ReservaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.aereopuerto.dto.TransaccionDTO;
import com.example.aereopuerto.dto.VentaDiariaDTO;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


@Service
@RequiredArgsConstructor
public class FacturaService {

    @Autowired
    private final FacturaRepository facturaRepository;
    private final ReservaRepository reservaRepository;

    public Factura obtenerFacturaPorId(Integer id) {
        return facturaRepository.findById(id).orElseThrow(() -> new FacturaInvalidaException("Factura no encontrada. ID: " + id));
    }

    public List<Factura> obtenerTodasLasFacturas() {
        return facturaRepository.findAll();
    }

    public List<Factura> obtenerFacturasPorPersona(Persona persona) {
        return facturaRepository.findByReservaPersona(persona);
    }

    public Factura crearFactura(Factura factura) {
        return facturaRepository.save(factura);
    }

    public Factura actualizarFactura(Integer id, FacturaDTO facturaDTO) {

        Factura f = facturaRepository.findById(id)
                .orElseThrow(() -> new FacturaInvalidaException("Factura no encontrada. ID: " + id));

        Reserva reserva = reservaRepository.findById(facturaDTO.getReservaId())
                .orElseThrow(() -> new RuntimeException(
                        "Reserva no encontrada. ID: " + facturaDTO.getReservaId()));

        f.setSituacionFiscal(facturaDTO.getSituacionFiscal());
        f.setReserva(reserva);
        f.setCUIL(facturaDTO.getCUIL());
        f.setFechaEmision(facturaDTO.getFechaEmision());
        f.setMetodoDePago(facturaDTO.getMetodoDePago());

        return facturaRepository.save(f);
    }

    public void eliminarFactura(Integer id) {
        facturaRepository.deleteById(id);
    }

    // =====================================================================
    //  Reportes de ventas (panel de administración)
    // =====================================================================

    private static final int MAX_DIAS_REPORTE = 366;

    /**
     * Ventas por día entre {@code desde} y {@code hasta} (ambos inclusive).
     * La suma se hace en la base de datos; acá solo se completan con 0
     * los días sin ventas para que el gráfico no tenga huecos.
     */
    public List<VentaDiariaDTO> obtenerVentasDiarias(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        Map<LocalDate, VentaDiariaDTO> porDia = new HashMap<>();
        for (Object[] fila : facturaRepository.ventasAgrupadasPorDia(desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())) {
            LocalDate fecha = aLocalDate(fila[0]);
            porDia.put(fecha, VentaDiariaDTO.builder()
                    .fecha(fecha)
                    .cantidad(((Number) fila[1]).longValue())
                    .total(((Number) fila[2]).doubleValue())
                    .build());
        }

        List<VentaDiariaDTO> resultado = new ArrayList<>();
        for (LocalDate dia = desde; !dia.isAfter(hasta); dia = dia.plusDays(1)) {
            resultado.add(porDia.getOrDefault(dia, new VentaDiariaDTO(dia, 0L, 0.0)));
        }
        return resultado;
    }

    /** Historial de transacciones (facturas) entre dos fechas, de la más nueva a la más vieja. */
    public List<TransaccionDTO> obtenerTransacciones(LocalDate desde, LocalDate hasta) {
        validarRango(desde, hasta);

        return facturaRepository
                .findByFechaEmisionGreaterThanEqualAndFechaEmisionLessThanOrderByFechaEmisionDesc(
                        desde.atStartOfDay(), hasta.plusDays(1).atStartOfDay())
                .stream()
                .map(this::aTransaccion)
                .toList();
    }

    private TransaccionDTO aTransaccion(Factura f) {
        Reserva r = f.getReserva();
        Persona p = r.getPersona();
        return TransaccionDTO.builder()
                .facturaId(f.getId())
                .fechaEmision(f.getFechaEmision())
                .reservaId(r.getId())
                .codigoReserva(r.getCodigoReserva())
                .cliente(p != null ? p.getApellido() + ", " + p.getNombre() : null)
                .cuil(f.getCUIL())
                .cantidadPasajes(r.getCantidadPasajes())
                .metodoDePago(f.getMetodoDePago())
                .cuotas(f.getCuotas())
                .total(f.getTotalPagado() != null ? f.getTotalPagado() : r.getValor().doubleValue())
                .estadoReserva(r.getEstadoReserva())
                .build();
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new IllegalArgumentException("La fecha 'desde' no puede ser posterior a 'hasta'.");
        }
        if (ChronoUnit.DAYS.between(desde, hasta) >= MAX_DIAS_REPORTE) {
            throw new IllegalArgumentException("El rango máximo del reporte es de " + MAX_DIAS_REPORTE + " días.");
        }
    }

    /** DATE() de MySQL puede llegar como java.sql.Date o como LocalDate según el driver. */
    private LocalDate aLocalDate(Object valor) {
        if (valor instanceof LocalDate ld) return ld;
        if (valor instanceof java.sql.Date d) return d.toLocalDate();
        if (valor instanceof LocalDateTime ldt) return ldt.toLocalDate();
        return LocalDate.parse(valor.toString().substring(0, 10));
    }
}
