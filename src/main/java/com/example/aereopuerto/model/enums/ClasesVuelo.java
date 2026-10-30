package com.example.aereopuerto.model.enums;

import com.fasterxml.jackson.annotation.JsonCreator;

public enum ClasesVuelo {
    TURISTA,
    ECONOMICA,
    BUSINESS,
    PRIMERA;

    @JsonCreator
    public static ClasesVuelo fromString(String value) {
        if (value == null) return null;
        return ClasesVuelo.valueOf(value.toUpperCase());
    }
}