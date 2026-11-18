package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.estadoVuelo;
import lombok.Data;
import org.springframework.cglib.core.Local;

import java.sql.Date;
import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
public class VueloDTO {
    private Integer aeropuertoOrigenId;
    private Integer aeropuertoDestinoId;
    private Integer avionId;
    private LocalDate fechaSalida;
    private LocalDate fechaLlegada;
    private LocalTime horaSalida;
    private LocalTime horaLlegada;
    private estadoVuelo estado;
    private Double precioVuelo;
    private Boolean escala;
    private Integer asientosPrimera;
    private Integer asientosEjecutiva;
    private Integer asientosEconomica;
}
