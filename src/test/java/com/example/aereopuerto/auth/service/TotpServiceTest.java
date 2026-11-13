package com.example.aereopuerto.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TotpServiceTest {

    private TotpService totpService;

    @BeforeEach
    void setUp() {
        totpService = new TotpService();
    }

    @Test
    @DisplayName("generateSecret produce un string Base32 de longitud correcta")
    void generateSecret_GeneraClaveBase32Valida() {
        String secret = totpService.generateSecret();
        assertNotNull(secret);
        assertTrue(secret.length() >= 16);
        assertTrue(secret.matches("[A-Z2-7]+"), "Debe contener solo caracteres de alfabeto Base32");
    }

    @Test
    @DisplayName("getOtpAuthUri construye URI compatible con Google Authenticator")
    void getOtpAuthUri_ConstruyeUriValida() {
        String secret = "JBSWY3DPEHPK3PXP";
        String uri = totpService.getOtpAuthUri(secret, "usuario@aerogest.com", "AeroGest");

        assertNotNull(uri);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=JBSWY3DPEHPK3PXP"));
        assertTrue(uri.contains("issuer=AeroGest"));
        assertTrue(uri.contains("digits=6"));
        assertTrue(uri.contains("period=30"));
    }

    @Test
    @DisplayName("getQrCodeUrl retorna URL apuntando a la API de QR")
    void getQrCodeUrl_RetornaUrlCorrecta() {
        String otpauth = "otpauth://totp/AeroGest:test?secret=TEST";
        String qrUrl = totpService.getQrCodeUrl(otpauth);

        assertNotNull(qrUrl);
        assertTrue(qrUrl.startsWith("https://api.qrserver.com/v1/create-qr-code/"));
    }

    @Test
    @DisplayName("verifyCode valida correctamente el código generado para el intervalo actual")
    void verifyCode_ConCodigoActual_RetornaTrue() {
        String secret = totpService.generateSecret();
        byte[] keyBytes = TotpService.decodeBase32(secret);
        long currentInterval = System.currentTimeMillis() / 1000 / 30;

        String code = totpService.generateTotp(keyBytes, currentInterval);
        assertNotNull(code);
        assertEquals(6, code.length());

        boolean valid = totpService.verifyCode(secret, code);
        assertTrue(valid);
    }

    @Test
    @DisplayName("verifyCode tolera ventana de tiempo de +/- 30 segundos")
    void verifyCode_ConVentanaTolerancia_RetornaTrue() {
        String secret = totpService.generateSecret();
        byte[] keyBytes = TotpService.decodeBase32(secret);
        long currentInterval = System.currentTimeMillis() / 1000 / 30;

        // Código del intervalo anterior (-30s)
        String prevCode = totpService.generateTotp(keyBytes, currentInterval - 1);
        assertTrue(totpService.verifyCode(secret, prevCode));

        // Código del siguiente intervalo (+30s)
        String nextCode = totpService.generateTotp(keyBytes, currentInterval + 1);
        assertTrue(totpService.verifyCode(secret, nextCode));
    }

    @Test
    @DisplayName("verifyCode rechaza código incorrecto o fuera de la ventana")
    void verifyCode_ConCodigoInvalidoOFueraDeVentana_RetornaFalse() {
        String secret = totpService.generateSecret();
        byte[] keyBytes = TotpService.decodeBase32(secret);
        long currentInterval = System.currentTimeMillis() / 1000 / 30;

        // Código fuera de ventana (hace 2 intervalos = -60s)
        String oldCode = totpService.generateTotp(keyBytes, currentInterval - 2);
        assertFalse(totpService.verifyCode(secret, oldCode));

        // Código de formato erróneo o inventado
        assertFalse(totpService.verifyCode(secret, "0000001"));
        assertFalse(totpService.verifyCode(secret, "abc"));
        assertFalse(totpService.verifyCode(secret, null));
    }

    @Test
    @DisplayName("encodeBase32 y decodeBase32 son reversibles")
    void base32_EsReversible() {
        byte[] original = "HolaMundo123456789".getBytes();
        String encoded = TotpService.encodeBase32(original);
        byte[] decoded = TotpService.decodeBase32(encoded);

        assertArrayEquals(original, decoded);
    }
}
