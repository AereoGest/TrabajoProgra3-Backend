package com.example.aereopuerto.dto;

/**
 * Resultado de validar los datos de un vuelo sin guardarlo.
 * Si {@code valido} es false, {@code mensaje} explica el primer problema encontrado.
 */
public record ValidacionVueloDTO(boolean valido, String mensaje) {

    public static ValidacionVueloDTO ok() {
        return new ValidacionVueloDTO(true, null);
    }

    public static ValidacionVueloDTO error(String mensaje) {
        return new ValidacionVueloDTO(false, mensaje);
    }
}
