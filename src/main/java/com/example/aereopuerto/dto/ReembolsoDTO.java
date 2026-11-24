package com.example.aereopuerto.dto;

import java.math.BigDecimal;

public record ReembolsoDTO(
        Integer reservaId,
        BigDecimal valorPagado,
        int porcentaje,
        BigDecimal montoReembolso,
        Long diasRestantes,
        boolean pagada,
        boolean cancelada) {
}