package com.example.aereopuerto.auth.dto;

import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.Sexo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompletarPerfilRequest {

    public static final LocalDate FECHA_NACIMIENTO_MINIMA = LocalDate.of(1900, 1, 1);

    @NotBlank(message = "El nombre es obligatorio.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El nombre solo puede contener letras y espacios.")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio.")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$", message = "El apellido solo puede contener letras y espacios.")
    private String apellido;

    @NotBlank(message = "El número de documento es obligatorio.")
    @Pattern(regexp = "^[0-9]+$", message = "El número de documento solo puede contener números positivos.")
    private String numeroIdentificador;

    @NotNull(message = "El tipo de identificador es obligatorio.")
    private Identificador identificador;

    @NotNull(message = "El sexo es obligatorio.")
    private Sexo sexo;

    @NotNull(message = "La fecha de nacimiento es obligatoria.")
    @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual.")
    private LocalDate fechaNacimiento;

    @JsonIgnore
    @AssertTrue(message = "La fecha de nacimiento no puede ser anterior al 01/01/1900.")
    public boolean isFechaNacimientoMinimaValida() {
        return fechaNacimiento == null || !fechaNacimiento.isBefore(FECHA_NACIMIENTO_MINIMA);
    }

    @NotBlank(message = "El teléfono es obligatorio.")
    @Pattern(
            regexp = "^\\d{10}$",
            message = "Ingrese un número de teléfono válido."
    )
    private String telefono;
}