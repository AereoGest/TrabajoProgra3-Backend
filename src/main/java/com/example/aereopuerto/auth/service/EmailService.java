package com.example.aereopuerto.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:https://aereogest.duckdns.org}")
    private String frontendUrl;

    @Async
    public void sendVerificationEmail(String toEmail, String verificationToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Verificación de Cuenta - Aerolínea");
        
        String verificationUrl = frontendUrl + "/verify?token=" + verificationToken;
        
        message.setText("Hola,\n\n" +
                "Gracias por registrarte en nuestra plataforma. " +
                "Por favor, verifica tu cuenta haciendo click en el siguiente enlace:\n\n" +
                verificationUrl + "\n\n" +
                "Saludos,\nEl equipo de AeroGest");
        
        mailSender.send(message);
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Restablecer Contraseña - Aerolínea");

        String resetUrl = frontendUrl + "/reset-password?token=" + resetToken;

        message.setText("Hola,\n\n" +
                "Recibimos una solicitud para restablecer tu contraseña. " +
                "Haz clic en el siguiente enlace para crear una nueva contraseña:\n\n" +
                resetUrl + "\n\n" +
                "Este enlace expirará en 1 hora.\n\n" +
                "Si no solicitaste este cambio, ignora este mensaje.\n\n" +
                "Saludos,\nEl equipo de AeroGest");

        mailSender.send(message);
    }

    @Async
    public void sendTwoFactorCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Código de Autenticacion 2FA");
        
        message.setText("Hola,\n\n" +
                "Tu código de autenticacion de dos factores es:\n\n" +
                code + "\n\n" +
                "Si no intentaste iniciar sesion, ignora este mensaje.\n\n" +
                "Saludos,\nEl equipo de AeroGest");

        mailSender.send(message);
    }

    /** Aviso al cliente de que su reserva se canceló porque la aerolínea canceló el vuelo. */
    @Async
    public void sendVueloCanceladoEmail(String toEmail, String nombre, String codigoReserva,
                                        String descripcionVuelo, BigDecimal montoReembolso) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Tu vuelo fue cancelado - Reserva " + codigoReserva);

        String saludo = (nombre == null || nombre.isBlank() || "Pendiente".equals(nombre))
                ? "Hola,\n\n" : "Hola " + nombre + ",\n\n";

        String reembolso = montoReembolso != null && montoReembolso.signum() > 0
                ? "Vamos a reintegrarte el 100% de lo abonado: " + formatearMonto(montoReembolso) + ".\n\n"
                : "Como la reserva todavía no estaba pagada, no se realizó ningún cobro.\n\n";

        message.setText(saludo +
                "Lamentamos informarte que el vuelo " + descripcionVuelo + " fue cancelado por la aerolínea.\n\n" +
                "Tu reserva " + codigoReserva + " quedó cancelada.\n" +
                reembolso +
                "Podés ver el detalle en Mis reservas: " + frontendUrl + "/reservas\n\n" +
                "Disculpá las molestias.\n\n" +
                "Saludos,\nEl equipo de AeroGest");

        mailSender.send(message);
    }

    private static String formatearMonto(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(monto);
    }
}
