package com.example.aereopuerto.auth.dto;

import com.example.aereopuerto.auth.entity.Role;
import com.example.aereopuerto.model.enums.Identificador;
import com.example.aereopuerto.model.enums.Sexo;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UsuarioResponse {
    private Integer id;
    private Integer personaId;
    private String nombre;
    private String apellido;
    private String email;
    private String telefono;
    private Role role;
    private Identificador identificador;
    private String numeroIdentificador;
    private LocalDate fechaNacimiento;
    private Sexo sexo;
}
