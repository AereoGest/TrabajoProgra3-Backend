package com.example.aereopuerto.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${mercadopago.access-token}")
    private String accessToken;

    @Value("${mercadopago.mock:false}")
    private boolean usarMock;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken != null ? accessToken.trim() : "");
        return headers;
    }

    public Map<String, Object> procesarPago(String token, Double amount, Integer installments,
                                            String paymentMethodId, String issuerId,
                                            String payerEmail, String payerDocType,
                                            String payerDocNumber, String descripcion, String externalReference) {

        if (usarMock || "mock".equalsIgnoreCase(token) || "test_token".equalsIgnoreCase(token)) {
            log.info("Procesando pago con mock para externalReference: {}", externalReference);
            Map<String, Object> mock = new HashMap<>();
            mock.put("status", "approved");
            mock.put("status_detail", "accredited");
            mock.put("id", System.currentTimeMillis());
            return mock;
        }

        Map<String, Object> payer = new HashMap<>();
        payer.put("email", (payerEmail != null && !payerEmail.isBlank()) ? payerEmail.trim() : "test_user@test.com");

        if (payerDocType != null && !payerDocType.isBlank() && payerDocNumber != null && !payerDocNumber.isBlank()) {
            Map<String, Object> identification = new HashMap<>();
            identification.put("type", payerDocType.trim());
            identification.put("number", payerDocNumber.trim());
            payer.put("identification", identification);
        }

        Double roundedAmount = (amount != null)
                ? BigDecimal.valueOf(amount).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", roundedAmount);
        body.put("token", token != null ? token.trim() : "");
        body.put("description", descripcion != null ? descripcion : "Reserva AeroGest");
        body.put("installments", installments != null && installments > 0 ? installments : 1);
        if (paymentMethodId != null && !paymentMethodId.isBlank()) {
            body.put("payment_method_id", paymentMethodId.toLowerCase().trim());
        }
        body.put("payer", payer);
        body.put("external_reference", externalReference);

        if (issuerId != null && !issuerId.isBlank()
                && !"null".equalsIgnoreCase(issuerId.trim())
                && !"undefined".equalsIgnoreCase(issuerId.trim())) {
            try {
                body.put("issuer_id", Long.parseLong(issuerId.trim()));
            } catch (NumberFormatException nfe) {
                body.put("issuer_id", issuerId.trim());
            }
        }

        try {
            log.info("Enviando pago a Mercado Pago para ref: {}, monto: {}, paymentMethod: {}",
                    externalReference, roundedAmount, paymentMethodId);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restClient.post()
                    .uri("https://api.mercadopago.com/v1/payments")
                    .headers(h -> {
                        h.addAll(authHeaders());
                        h.set("X-Idempotency-Key", UUID.randomUUID().toString());
                    })
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            log.info("Respuesta de Mercado Pago para ref {}: status={}",
                    externalReference, response != null ? response.get("status") : "null");
            return response != null ? response : Map.of("status", "error", "error", "Respuesta vacia de Mercado Pago");
        } catch (RestClientResponseException e) {
            String errorBody = e.getResponseBodyAsString();
            log.error("Mercado Pago respondio error HTTP {}: {}", e.getStatusCode().value(), errorBody);
            String mensajeDetalle = parsearErrorMercadoPago(errorBody, e.getStatusCode().value());
            return Map.of("status", "error", "error", mensajeDetalle);
        } catch (RestClientException e) {
            log.error("Error de comunicacion con Mercado Pago: {}", e.getMessage(), e);
            return Map.of("status", "error", "error", "No se pudo comunicar con Mercado Pago");
        }
    }

    private String parsearErrorMercadoPago(String body, int statusCode) {
        if (body == null || body.isBlank()) {
            return "Error al procesar el pago en Mercado Pago (HTTP " + statusCode + ")";
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.has("message") && !root.get("message").asText().isBlank()) {
                String msg = root.get("message").asText();
                if (root.has("cause") && root.get("cause").isArray() && !root.get("cause").isEmpty()) {
                    JsonNode firstCause = root.get("cause").get(0);
                    if (firstCause.has("description") && !firstCause.get("description").asText().isBlank()) {
                        return firstCause.get("description").asText();
                    }
                }
                return msg;
            }
        } catch (Exception ignored) {
        }
        return "Mercado Pago respondio " + statusCode + ": " + body;
    }
}
