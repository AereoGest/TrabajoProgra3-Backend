package com.example.aereopuerto.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TotpVerifyRequest {

    @NotBlank(message = "El secreto es requerido")
    private String secret;

    @NotBlank(message = "El código es requerido")
    private String code;
}
