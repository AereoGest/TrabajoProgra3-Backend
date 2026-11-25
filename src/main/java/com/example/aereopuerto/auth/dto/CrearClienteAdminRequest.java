package com.example.aereopuerto.auth.dto;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.Sexo;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CrearClienteAdminRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
    @Pattern(regexp = ReglasDatosPersonales.SOLO_LETRAS, message = "El nombre solo puede contener letras y espacios")
    private String nombre;

    @NotBlank(message = "El apellido es obligatorio")
    @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
    @Pattern(regexp = ReglasDatosPersonales.SOLO_LETRAS, message = "El apellido solo puede contener letras y espacios")
    private String apellido;

    @NotNull(message = "El tipo de identificación es obligatorio")
    private Identificador identificador;

    @NotBlank(message = "El número de documento es obligatorio")
    @Size(max = 50, message = "El documento no puede superar los 50 caracteres")
    private String numeroIdentificador;

    @JsonIgnore
    @AssertTrue(message = "El DNI debe tener 7 u 8 números y el pasaporte entre 6 y 20 letras mayúsculas o números")
    public boolean isDocumentoValido() {
        return ReglasDatosPersonales.documentoValido(identificador, numeroIdentificador);
    }

    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
    private LocalDate fechaNacimiento;

    @JsonIgnore
    @AssertTrue(message = "La fecha de nacimiento no puede ser anterior al 01/01/1900")
    public boolean isFechaNacimientoMinimaValida() {
        return fechaNacimiento == null || !fechaNacimiento.isBefore(CompletarPerfilRequest.FECHA_NACIMIENTO_MINIMA);
    }

    @NotNull(message = "El sexo es obligatorio")
    private Sexo sexo;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "El formato de email es inválido")
    private String email;

    @NotBlank(message = "El teléfono es obligatorio")
    @Pattern(regexp = ReglasDatosPersonales.TELEFONO, message = "Ingrese un número de teléfono válido (10 dígitos, sin símbolos)")
    private String telefono;

    @NotBlank(message = "La contraseña es obligatoria")
    @Pattern(regexp = ReglasDatosPersonales.PASSWORD, message = ReglasDatosPersonales.MENSAJE_PASSWORD)
    private String password;

    @NotNull(message = "El rol es obligatorio")
    private Role role;
}
