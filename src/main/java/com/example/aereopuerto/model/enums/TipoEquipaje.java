package com.example.aereopuerto.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum TipoEquipaje {
    CABINA, BODEGA, MANO;


    @JsonCreator
    public static TipoEquipaje fromString(String value) {
        if (value == null) return null;
        return TipoEquipaje.valueOf(value.toUpperCase());
    }
}