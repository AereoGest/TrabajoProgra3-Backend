package com.example.aereopuerto.auth.dto;

import com.example.aereopuerto.auth.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

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
}
