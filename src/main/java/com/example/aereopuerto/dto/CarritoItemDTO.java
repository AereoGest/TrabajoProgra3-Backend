package com.example.aereopuerto.dto;

import com.example.aereopuerto.model.enums.ClasesVuelo;
import lombok.Data;

@Data
public class CarritoItemDTO {
    private Integer id;
    private Integer carritoId;
    private Integer vueloId;
    private Integer cantidad;
    private ClasesVuelo claseVuelo;
    /** Asientos libres que quedan en el vuelo: tope para la cantidad de pasajes. */
    private Integer asientosDisponibles;
}
