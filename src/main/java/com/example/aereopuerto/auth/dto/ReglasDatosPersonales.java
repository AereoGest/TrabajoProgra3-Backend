package com.example.aereopuerto.auth.dto;

import com.example.aereopuerto.model.enums.Identificador;

/**
 * Reglas de formato de los datos personales, compartidas por el registro/perfil del usuario
 * y por los formularios de alta y edición de clientes del panel de administración.
 */
public final class ReglasDatosPersonales {

    private ReglasDatosPersonales() {
    }

    /** Solo letras (con tildes y ñ) y espacios. */
    public static final String SOLO_LETRAS = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ ]+$";

    /** Teléfono de 10 dígitos, sin símbolos (ej.: 2235001122). */
    public static final String TELEFONO = "^\\d{10}$";

    /** Al menos 8 caracteres, una mayúscula, una minúscula y un carácter especial. */
    public static final String PASSWORD = "^(?=.*[a-z])(?=.*[A-Z])(?=.*[^a-zA-Z0-9]).{8,}$";
    public static final String MENSAJE_PASSWORD =
            "La contraseña debe tener al menos 8 caracteres, una mayúscula, una minúscula y un carácter especial";

    private static final String DNI = "^\\d{7,8}$";
    private static final String PASAPORTE = "^[A-Z0-9]{6,20}$";

    /** DNI: 7 u 8 dígitos. Pasaporte: 6 a 20 letras mayúsculas o números. Null se deja a @NotNull/@NotBlank. */
    public static boolean documentoValido(Identificador tipo, String numero) {
        if (tipo == null || numero == null || numero.isBlank()) {
            return true;
        }
        String limpio = numero.trim();
        return tipo == Identificador.DNI ? limpio.matches(DNI) : limpio.matches(PASAPORTE);
    }
}
