package com.example.aereopuerto.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pasajecheckindto {

    private Integer pasajeId;
    private String pasajero;
    private Integer vueloId;
    private String asiento;
    private boolean asignadoAutomaticamente;
}
