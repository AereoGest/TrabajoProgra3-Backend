package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.ClasesVuelo;
import lombok.Data;

@Data
public class AsientoDTO {
    private Integer id;
    private String codigo;
    private Double precioExtra;
    private Integer avionId;
    private Integer vueloId;
    private ClasesVuelo categoria;
    private Boolean ocupado;
}
