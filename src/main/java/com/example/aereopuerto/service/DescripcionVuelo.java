package com.example.aereopuerto.service;

import com.example.aereopuerto.model.Vuelo;

import java.time.format.DateTimeFormatter;

/** Texto legible de un vuelo para los mails que se le mandan al cliente. */
final class DescripcionVuelo {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm");

    private DescripcionVuelo() {
    }

    /** Ej.: "Buenos Aires (EZE) → Madrid (MAD) del 15/12/2026 a las 10:00". */
    static String de(Vuelo vuelo) {
        StringBuilder sb = new StringBuilder();
        if (vuelo.getAeropuertoOrigen() != null && vuelo.getAeropuertoDestino() != null) {
            sb.append(vuelo.getAeropuertoOrigen().getCiudad())
                    .append(" (").append(vuelo.getAeropuertoOrigen().getCodigoIata()).append(") → ")
                    .append(vuelo.getAeropuertoDestino().getCiudad())
                    .append(" (").append(vuelo.getAeropuertoDestino().getCodigoIata()).append(")");
        } else {
            sb.append("#").append(vuelo.getId());
        }
        if (vuelo.getFechaSalida() != null) {
            sb.append(" del ").append(vuelo.getFechaSalida().format(FECHA));
        }
        if (vuelo.getHoraSalida() != null) {
            sb.append(" a las ").append(vuelo.getHoraSalida().format(HORA));
        }
        return sb.toString();
    }
}
