package com.example.aereopuerto.auth.service;

import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Servicio para autenticación móvil basada en tiempo (TOTP - RFC 6238 / RFC 4226),
 * compatible con Google Authenticator, Microsoft Authenticator, Authy, etc.
 */
@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int SECRET_BYTE_LENGTH = 20; // 160 bits
    private static final int TIME_STEP_SECONDS = 30;
    private static final int CODE_DIGITS = 6;
    private static final String HMAC_ALGO = "HmacSHA1";

    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Genera un secreto aleatorio criptográficamente seguro en formato Base32.
     */
    public String generateSecret() {
        byte[] buffer = new byte[SECRET_BYTE_LENGTH];
        secureRandom.nextBytes(buffer);
        return encodeBase32(buffer);
    }

    /**
     * Construye la URI otpauth estándar para escaneo de código QR en aplicaciones móviles.
     */
    public String getOtpAuthUri(String secret, String email, String issuer) {
        String safeIssuer = (issuer == null || issuer.isBlank()) ? "AeroGest" : issuer.trim();
        String safeEmail = (email == null || email.isBlank()) ? "usuario@aerogest.com" : email.trim();
        String encodedIssuer = URLEncoder.encode(safeIssuer, StandardCharsets.UTF_8);
        String encodedLabel = URLEncoder.encode(safeIssuer + ":" + safeEmail, StandardCharsets.UTF_8);

        return String.format(
                "otpauth://totp/%s?secret=%s&issuer=%s&algorithm=SHA1&digits=%d&period=%d",
                encodedLabel, secret.trim().toUpperCase(), encodedIssuer, CODE_DIGITS, TIME_STEP_SECONDS
        );
    }

    /**
     * Genera la URL pública de la imagen QR para renderizar en el frontend.
     */
    public String getQrCodeUrl(String otpAuthUri) {
        String encoded = URLEncoder.encode(otpAuthUri, StandardCharsets.UTF_8);
        return "https://api.qrserver.com/v1/create-qr-code/?size=220x220&data=" + encoded;
    }

    /**
     * Verifica si el código de 6 dígitos ingresado por el usuario es válido
     * para el secreto dado dentro de la ventana de tiempo permitida (+/- 1 intervalo).
     */
    public boolean verifyCode(String base32Secret, String code) {
        if (base32Secret == null || base32Secret.isBlank() || code == null) {
            return false;
        }

        String cleanCode = code.replaceAll("[\\s-]", "");
        if (!cleanCode.matches("\\d{" + CODE_DIGITS + "}")) {
            return false;
        }

        try {
            byte[] keyBytes = decodeBase32(base32Secret.trim().toUpperCase());
            long currentInterval = System.currentTimeMillis() / 1000 / TIME_STEP_SECONDS;

            // Ventana de tolerancia: intervalo anterior (-30s), actual, y siguiente (+30s)
            for (int i = -1; i <= 1; i++) {
                String candidate = generateTotp(keyBytes, currentInterval + i);
                if (cleanCode.equals(candidate)) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /**
     * Genera el código TOTP numérico de 6 dígitos para un intervalo dado.
     */
    public String generateTotp(byte[] keyBytes, long interval) {
        try {
            byte[] data = new byte[8];
            long value = interval;
            for (int i = 7; i >= 0; i--) {
                data[i] = (byte) (value & 0xFF);
                value >>= 8;
            }

            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(keyBytes, HMAC_ALGO));
            byte[] hash = mac.doFinal(data);

            int offset = hash[hash.length - 1] & 0x0F;
            int binary = ((hash[offset] & 0x7F) << 24)
                    | ((hash[offset + 1] & 0xFF) << 16)
                    | ((hash[offset + 2] & 0xFF) << 8)
                    | (hash[offset + 3] & 0xFF);

            int otp = binary % (int) Math.pow(10, CODE_DIGITS);
            return String.format("%0" + CODE_DIGITS + "d", otp);
        } catch (Exception e) {
            throw new RuntimeException("Error al calcular código TOTP", e);
        }
    }

    // --- Implementación estándar RFC 4648 Base32 ---

    public static String encodeBase32(byte[] data) {
        StringBuilder result = new StringBuilder((data.length * 8 + 4) / 5);
        int buffer = 0;
        int next = 0;
        int bitsLeft = 0;

        while (next < data.length) {
            buffer <<= 8;
            buffer |= (data[next++] & 0xFF);
            bitsLeft += 8;
            while (bitsLeft >= 5) {
                int index = (buffer >> (bitsLeft - 5)) & 0x1F;
                bitsLeft -= 5;
                result.append(BASE32_ALPHABET.charAt(index));
            }
        }

        if (bitsLeft > 0) {
            int index = (buffer << (5 - bitsLeft)) & 0x1F;
            result.append(BASE32_ALPHABET.charAt(index));
        }

        return result.toString();
    }

    public static byte[] decodeBase32(String base32) {
        String clean = base32.replaceAll("[=\\s-]", "").toUpperCase();
        byte[] bytes = new byte[clean.length() * 5 / 8];
        int buffer = 0;
        int bitsLeft = 0;
        int count = 0;

        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            int val = BASE32_ALPHABET.indexOf(c);
            if (val < 0) {
                throw new IllegalArgumentException("Carácter Base32 inválido: " + c);
            }
            buffer <<= 5;
            buffer |= val;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                bytes[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xFF);
                bitsLeft -= 8;
            }
        }
        return bytes;
    }
}
