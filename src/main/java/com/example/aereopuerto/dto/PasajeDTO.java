package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.ClasesVuelo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class PasajeDTO {

    private String codigoPasaje;
    private Integer reservaId;

    @NotNull
    private Integer vueloId;
    private Integer asientoId;
    @NotNull
    private ClasesVuelo clasesVuelo;
    private List<Integer> equipajeIds;
    private Integer asistenciaId;

    @NotBlank
    private String nombre;
    @NotBlank
    private String apellido;
    @NotBlank
    private String documento;
    private LocalDate fechaNacimiento;
    private String email;

}
