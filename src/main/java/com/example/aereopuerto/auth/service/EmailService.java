package com.example.aereopuerto.auth.service;

import com.example.aereopuerto.service.CompraConfirmadaEvent;
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

    @Value("${app.frontend.url:https://aerogest.ddns.net}")
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

        String saludo = saludo(nombre);

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

    /** Recibo que se le manda al cliente cuando se aprueba el pago de su reserva. */
    @Async
    public void sendReciboCompraEmail(CompraConfirmadaEvent recibo) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(recibo.email());
        message.setSubject("Recibo de tu compra - Reserva " + recibo.codigoReserva());
        message.setText(armarTextoRecibo(recibo));
        mailSender.send(message);
    }

    String armarTextoRecibo(CompraConfirmadaEvent recibo) {
        StringBuilder sb = new StringBuilder(saludo(recibo.nombre()));
        sb.append("¡Gracias por tu compra! Tu pago fue aprobado y tu reserva quedó confirmada.\n\n");
        sb.append("Reserva: ").append(recibo.codigoReserva()).append("\n");
        if (recibo.descripcionVuelo() != null) {
            sb.append("Vuelo: ").append(recibo.descripcionVuelo()).append("\n");
        }

        if (recibo.pasajeros() != null && !recibo.pasajeros().isEmpty()) {
            sb.append("\nPasajeros:\n");
            for (CompraConfirmadaEvent.Pasajero p : recibo.pasajeros()) {
                sb.append("  - ").append(p.nombreCompleto());
                if (p.clase() != null) sb.append(" · ").append(p.clase());
                sb.append(" · ").append(p.asiento() != null ? "Asiento " + p.asiento() : "Asiento a asignar en el check-in");
                sb.append("\n");
            }
        }

        sb.append("\nTotal pagado: ").append(recibo.total() != null ? formatearMonto(recibo.total()) : "-").append("\n");
        if (recibo.medioDePago() != null) {
            sb.append("Medio de pago: ").append(recibo.medioDePago());
            if (recibo.cuotas() != null && recibo.cuotas() > 1) {
                sb.append(" en ").append(recibo.cuotas()).append(" cuotas");
            }
            sb.append("\n");
        }

        sb.append("\nPodés ver el detalle y descargar tu factura en Mis reservas: ")
                .append(frontendUrl).append("/reservas\n\n")
                .append("¡Buen viaje!\n\n")
                .append("Saludos,\nEl equipo de AeroGest");
        return sb.toString();
    }

    private static String saludo(String nombre) {
        return (nombre == null || nombre.isBlank() || "Pendiente".equals(nombre))
                ? "Hola,\n\n" : "Hola " + nombre + ",\n\n";
    }

    private static String formatearMonto(BigDecimal monto) {
        return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("es-AR")).format(monto);
    }
}
