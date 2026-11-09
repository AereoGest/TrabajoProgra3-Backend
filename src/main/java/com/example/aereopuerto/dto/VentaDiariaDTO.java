package com.example.aereopuerto.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Total vendido en un día (suma de las facturas emitidas ese día).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VentaDiariaDTO {
    private LocalDate fecha;
    private Long cantidad;   // cantidad de transacciones (facturas)
    private Double total;    // monto total cobrado
}