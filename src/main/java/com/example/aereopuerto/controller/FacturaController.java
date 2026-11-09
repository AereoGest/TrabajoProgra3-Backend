package com.example.aereopuerto.controller;

import com.example.aereopuerto.auth.entity.User;
import com.example.aereopuerto.dto.FacturaDTO;
import com.example.aereopuerto.model.Factura;
import com.example.aereopuerto.service.FacturaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.example.aereopuerto.dto.TransaccionDTO;
import com.example.aereopuerto.dto.VentaDiariaDTO;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
@Tag(name = "Facturas", description = "Operaciones relacionadas con las facturas")
public class FacturaController {

    @Autowired
    private final FacturaService facturaService;

    @Operation(
            summary = "Obtener factura por ID",
            description = "Devuelve los datos de una factura específica"
    )
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Factura> obtenerFactura(@PathVariable Integer id) {
        return ResponseEntity.ok(facturaService.obtenerFacturaPorId(id));
    }

    @Operation(
            summary = "Obtener mis facturas",
            description = "Devuelve las facturas del usuario autenticado"
    )
    @GetMapping
    public ResponseEntity<List<Factura>> obtenerMisFacturas(@AuthenticationPrincipal User usuarioAutenticado) {
        return ResponseEntity.ok(facturaService.obtenerFacturasPorPersona(usuarioAutenticado.getPersona()));
    }

    @Operation(
            summary = "Obtener todas las facturas",
            description = "Solo empleados y administradores"
    )
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<List<Factura>> obtenerTodas() {
        return ResponseEntity.ok(facturaService.obtenerTodasLasFacturas());
    }

    @Operation(
            summary = "Ventas diarias",
            description = "Total vendido y cantidad de transacciones por día entre dos fechas (inclusive). "
                    + "Si no se mandan fechas, devuelve los últimos 30 días. Solo ADMIN."
    )
    @GetMapping("/admin/ventas-diarias")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<VentaDiariaDTO>> ventasDiarias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        LocalDate d = desde != null ? desde : h.minusDays(29);
        return ResponseEntity.ok(facturaService.obtenerVentasDiarias(d, h));
    }

    @Operation(
            summary = "Historial de transacciones",
            description = "Facturas emitidas entre dos fechas (inclusive), de la más reciente a la más antigua. "
                    + "Si no se mandan fechas, devuelve los últimos 30 días. Solo ADMIN."
    )
    @GetMapping("/admin/transacciones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransaccionDTO>> transacciones(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        LocalDate h = hasta != null ? hasta : LocalDate.now();
        LocalDate d = desde != null ? desde : h.minusDays(29);
        return ResponseEntity.ok(facturaService.obtenerTransacciones(d, h));
    }

    @Operation(summary = "Crear factura", description = "Registra una nueva factura.")
    @PostMapping
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Factura> crearFactura(@RequestBody Factura factura) {
        Factura nueva = facturaService.crearFactura(factura);
        return ResponseEntity.status(HttpStatus.CREATED).body(nueva);
    }

    @Operation(summary = "Actualizar factura", description = "Actualiza una factura existente.")
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Factura> actualizarFactura(@PathVariable Integer id, @RequestBody FacturaDTO factura) {
        return ResponseEntity.ok(facturaService.actualizarFactura(id,factura));
    }

    @Operation(summary = "Eliminar factura", description = "Elimina una factura.")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLEADO', 'ADMIN')")
    public ResponseEntity<Void> eliminarFactura(@PathVariable Integer id) {
        facturaService.eliminarFactura(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<String> descargarFactura(@PathVariable Integer id) {
        Factura f = facturaService.obtenerFacturaPorId(id);
        String metodoLabel = switch (f.getMetodoDePago() != null ? f.getMetodoDePago().name() : "") {
            case "TARJETA_CREDITO" -> "Tarjeta de crédito";
            case "TARJETA_DEBITO" -> "Tarjeta de débito";
            case "TRANSFERENCIA" -> "Transferencia bancaria";
            case "MERCADOPAGO" -> "Mercado Pago";
            default -> f.getMetodoDePago() != null ? f.getMetodoDePago().name() : "—";
        };
        String reservaInfo = "";
        if (f.getReserva() != null) {
            reservaInfo = String.format("""
                <tr><td><strong>Reserva N°</strong></td><td>%d</td></tr>
                <tr><td><strong>Pasajes</strong></td><td>%d</td></tr>
                <tr><td><strong>Total</strong></td><td><strong>$%.2f</strong></td></tr>
            """, f.getReserva().getId(), f.getReserva().getCantidadPasajes(), f.getReserva().getValor());
            if (f.getReserva().getPersona() != null) {
                reservaInfo = String.format("""
                    <tr><td><strong>Cliente</strong></td><td>%s %s</td></tr>
                """, f.getReserva().getPersona().getNombre(), f.getReserva().getPersona().getApellido()) + reservaInfo;
            }
        }
        String cuotasInfo = "";
        if (f.getCuotas() != null && f.getCuotas() > 1) {
            cuotasInfo = String.format("""
                <tr><td><strong>Cuotas</strong></td><td>%d cuotas de <strong>$%.2f</strong></td></tr>
            """, f.getCuotas(), (f.getTotalPagado() != null ? f.getTotalPagado() : 0) / f.getCuotas());
        }
        String html = """
            <!DOCTYPE html>
            <html lang="es">
            <head><meta charset="UTF-8"><title>Factura #%d</title>
            <style>
                * { margin:0; padding:0; box-sizing:border-box; }
                body { font-family:'Segoe UI',Arial,sans-serif; color:#1a1a2e; padding:40px; }
                .invoice { max-width:800px; margin:0 auto; border:1px solid #e0e0e0; border-radius:12px; padding:48px; }
                .header { display:flex; justify-content:space-between; align-items:start; margin-bottom:40px;
                          padding-bottom:24px; border-bottom:2px solid #1a1a2e; }
                .title { font-size:28px; font-weight:800; letter-spacing:-0.5px; }
                .title span { color:#6c5ce7; }
                .meta { font-size:13px; color:#666; margin-top:4px; }
                table { width:100%%; border-collapse:collapse; margin:24px 0; }
                td { padding:10px 8px; border-bottom:1px solid #eee; font-size:14px; }
                td:last-child { text-align:right; }
                .footer { margin-top:32px; padding-top:16px; border-top:1px solid #e0e0e0;
                          font-size:12px; color:#999; text-align:center; }
                @media print { body { padding:0; } .invoice { border:none; } }
            </style></head>
            <body><div class="invoice">
                <div class="header">
                    <div><div class="title">Aero<span>Gest</span></div>
                         <div class="meta">Comprobante de pago</div></div>
                    <div style="text-align:right">
                        <div style="font-size:22px;font-weight:700">#%d</div>
                        <div class="meta">%s</div>
                    </div>
                </div>
                <table>
                    %s
                    %s
                    <tr><td><strong>CUIL</strong></td><td>%s</td></tr>
                    <tr><td><strong>Situación fiscal</strong></td><td>%s</td></tr>
                    <tr><td><strong>Método de pago</strong></td><td>%s</td></tr>
                </table>
                <div class="footer">Generado el %s &mdash; AeroGest</div>
            </div></body></html>
        """.formatted(
                f.getId(),
                f.getId(),
                f.getFechaEmision() != null ?
                        java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").format(f.getFechaEmision()) : "—",
                reservaInfo,
                cuotasInfo,
                f.getCUIL() != null ? f.getCUIL() : "—",
                f.getSituacionFiscal() != null ? f.getSituacionFiscal() : "—",
                metodoLabel,
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        );
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=factura-" + f.getId() + ".html")
                .contentType(MediaType.TEXT_HTML)
                .body(html);
    }
}
