package com.example.aereopuerto.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AeropuertoDTO {

    @NotBlank(message = "El nombre es obligatorio.")
    @Pattern(
            regexp = "^(?=(?:.*[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ]){4,})[a-zA-ZáéíóúÁÉÍÓÚüÜñÑ ]+$",
            message = "El nombre solo puede contener letras y espacios, con al menos 4 letras."
    )
    private String nombre;

    @NotBlank(message = "El código IATA es obligatorio.")
    @Pattern(regexp = "^[A-Za-z]{3}$", message = "El código IATA debe tener exactamente 3 letras.")
    private String codigoIata;

    @NotBlank(message = "La ciudad es obligatoria.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "La ciudad solo puede contener letras y espacios.")
    private String ciudad;

    @NotBlank(message = "El país es obligatorio.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El país solo puede contener letras y espacios.")
    private String pais;
}