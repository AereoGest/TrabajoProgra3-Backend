package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.ClasesVuelo;
import com.example.aereopuerto.model.enums.Identificador;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PasajeRequest {
    @NotNull(message = "la clase es obligatoria")
    private ClasesVuelo clasesVuelo;

    private Integer asientoId;              // opcional: se puede asignar en el check-in
    private List<Integer> equipajeIds;      // por pasajero
    private Integer asistenciaId;           // por pasajero

    @NotBlank(message = "el nombre es obligatorio")
    @Size(max = 100, message = "máximo 100 caracteres")
    private String nombre;

    @NotBlank(message = "el apellido es obligatorio")
    @Size(max = 100, message = "máximo 100 caracteres")
    private String apellido;

    @NotNull(message = "el tipo de documento es obligatorio")
    private Identificador tipoDocumento;

    @NotBlank(message = "el documento es obligatorio")
    @Size(max = 20, message = "máximo 20 caracteres")
    private String documento;

    @NotNull(message = "la fecha de nacimiento es obligatoria")
    @Past(message = "tiene que ser una fecha pasada")
    private LocalDate fechaNacimiento;

    @Email(message = "email inválido")
    @Size(max = 100, message = "máximo 100 caracteres")
    private String email;

}
