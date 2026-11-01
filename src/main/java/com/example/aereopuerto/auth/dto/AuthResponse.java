package com.example.aereopuerto.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    private String token;
    private String email;
    private Integer userId;
    private Boolean perfilCompleto;

    private Boolean requires2fa;
    private String role;
    private Boolean isVerified;

    public AuthResponse(String nuevoToken) {
        this.token = nuevoToken;
    }
}
