package com.example.aereopuerto.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReservaRequest {
    private Integer personaId;

    @NotNull(message = "el vuelo es obligatorio")
    private Integer vueloId;

    @NotNull(message = "la cantidad de pasajes es obligatoria")
    @Min(value = 1, message = "tiene que ser al menos 1")
    private Integer cantidadPasajes;

    @NotEmpty(message = "tenés que cargar al menos un pasajero")
    @Valid
    private List<PasajeRequest> pasajes;
}
