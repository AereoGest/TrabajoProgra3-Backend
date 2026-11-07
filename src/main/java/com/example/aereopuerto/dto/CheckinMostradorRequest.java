package com.example.aereopuerto.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinMostradorRequest {

    @NotBlank(message = "el código de reserva es obligatorio")
    private String codigoReserva;

    @NotBlank(message = "el documento es obligatorio")
    private String documento;
}