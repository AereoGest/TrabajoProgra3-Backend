package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.EstadoReserva;
import com.example.aereopuerto.model.enums.MetodosDePago;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Una transacción del historial de ventas (una factura) con solo los datos
 * que necesita el panel de administración.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransaccionDTO {
    private Integer facturaId;
    private LocalDateTime fechaEmision;
    private Integer reservaId;
    private String codigoReserva;
    private String cliente;
    private String cuil;
    private Integer cantidadPasajes;
    private MetodosDePago metodoDePago;
    private Integer cuotas;
    private Double total;
    private EstadoReserva estadoReserva;
}