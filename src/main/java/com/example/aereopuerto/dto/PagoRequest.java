package com.example.aereopuerto.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PagoRequest {
    @NotBlank(message = "falta el token de la tarjeta")
    private String token;

    @NotNull
    @Min(1)
    private Integer installments;

    @NotBlank(message = "falta el medio de pago")
    private String paymentMethodId;

    private String issuerId;

    @NotBlank @Email
    private String payerEmail;

    @NotBlank
    private String payerDocType;

    @NotBlank
    private String payerDocNumber;

    @NotBlank(message = "el CUIL/CUIT es obligatorio")
    private String cuil;

    @NotBlank(message = "la situación fiscal es obligatoria")
    private String situacionFiscal;
}
