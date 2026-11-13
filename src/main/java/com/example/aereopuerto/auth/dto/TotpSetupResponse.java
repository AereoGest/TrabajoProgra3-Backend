package com.example.aereopuerto.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TotpSetupResponse {
    private String secret;
    private String otpauthUrl;
    private String qrCodeUrl;
}
