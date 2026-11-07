package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.EstadoReserva;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReservaCheckinDTO {

    private Integer reservaId;
    private String codigoReserva;
    private EstadoReserva estado;
    private LocalDateTime fechaCheckIn;
    private String titular;
    private List<PasajeroCheckin> pasajes;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PasajeroCheckin {
        private Integer pasajeId;
        private String pasajero;
        private String documento;
        private Integer vueloId;
        private String origen;
        private String destino;
        private LocalDate fechaSalida;
        private LocalTime horaSalida;
        private String asiento;
    }
}