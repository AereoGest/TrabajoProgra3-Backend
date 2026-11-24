package com.example.aereopuerto.auth.dto;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.Sexo;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/** El alta y la edición de clientes del admin aplican las mismas reglas que el registro del usuario. */
class ClienteAdminRequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void init() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    private CrearClienteAdminRequest.CrearClienteAdminRequestBuilder altaValida() {
        return CrearClienteAdminRequest.builder()
                .nombre("María José").apellido("Núñez")
                .identificador(Identificador.DNI).numeroIdentificador("30111222")
                .fechaNacimiento(LocalDate.of(1990, 5, 10)).sexo(Sexo.FEMENINO)
                .email("maria@correo.com").telefono("2235001122")
                .password("Clave#123").role(Role.ROLE_USUARIO);
    }

    private ActualizarClienteAdminRequest.ActualizarClienteAdminRequestBuilder edicionValida() {
        return ActualizarClienteAdminRequest.builder()
                .nombre("Juan").apellido("Pérez")
                .identificador(Identificador.PASAPORTE).numeroIdentificador("AAB123456")
                .fechaNacimiento(LocalDate.of(1995, 3, 10)).sexo(Sexo.MASCULINO)
                .email("juan@correo.com").telefono("1144332211")
                .role(Role.ROLE_USUARIO);
    }

    private static <T> Set<String> camposConError(T request) {
        return validator.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Datos válidos: el alta y la edición pasan (la contraseña es opcional al editar)")
    void datosValidos() {
        assertTrue(camposConError(altaValida().build()).isEmpty());
        assertTrue(camposConError(edicionValida().build()).isEmpty());
    }

    @Test
    @DisplayName("Nombre y apellido: rechaza HTML, números y símbolos")
    void nombreSoloLetras() {
        assertEquals(Set.of("nombre"), camposConError(altaValida().nombre("<script>alert(1)</script>").build()));
        assertEquals(Set.of("apellido"), camposConError(altaValida().apellido("Pérez2").build()));
        assertEquals(Set.of("nombre"), camposConError(edicionValida().nombre("Juan<b>").build()));
    }

    @Test
    @DisplayName("Teléfono: exactamente 10 dígitos, sin símbolos")
    void telefonoDiezDigitos() {
        assertEquals(Set.of("telefono"), camposConError(altaValida().telefono("+542235001122").build()));
        assertEquals(Set.of("telefono"), camposConError(edicionValida().telefono("223 500 1122").build()));
        assertEquals(Set.of("telefono"), camposConError(altaValida().telefono("123456789").build()));
    }

    @Test
    @DisplayName("Contraseña: misma regla que el registro, también al editar si se manda")
    void passwordComoRegistro() {
        assertEquals(Set.of("password"), camposConError(altaValida().password("clave123").build()));
        assertEquals(Set.of("password"), camposConError(edicionValida().password("Corta#1").build()));
    }

    @Test
    @DisplayName("Documento: DNI de 7 u 8 números, pasaporte de 6 a 20 letras mayúsculas o números")
    void documentoSegunTipo() {
        assertEquals(Set.of("documentoValido"), camposConError(altaValida().numeroIdentificador("30.111.222").build()));
        assertEquals(Set.of("documentoValido"), camposConError(altaValida().numeroIdentificador("123456").build()));
        assertEquals(Set.of("documentoValido"), camposConError(edicionValida().numeroIdentificador("ab-12").build()));
    }
}
