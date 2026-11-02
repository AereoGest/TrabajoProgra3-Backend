package com.example.aereopuerto.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MercadoPagoService {

    private final RestClient restClient;

    @Value("${mercadopago.access-token}")
    private String accessToken;
    @Value("${mercadopago.mock:false}")
    private boolean usarMock;

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(accessToken);
        return headers;
    }

    public Map<String, Object> procesarPago(String token, Double amount, Integer installments,
                                            String paymentMethodId, String issuerId,
                                            String payerEmail, String payerDocType,
                                            String payerDocNumber, String descripcion, String externalReference) {

        if (usarMock) {
            Map<String, Object> mock = new HashMap<>();
            mock.put("status", "approved");
            mock.put("id", 123456789);
            return mock;
        }

        Map<String, Object> identification = Map.of(
                "type", payerDocType,
                "number", payerDocNumber);
        Map<String, Object> payer = Map.of(
                "email", payerEmail,
                "identification", identification);

        Map<String, Object> body = new HashMap<>();
        body.put("transaction_amount", amount);
        body.put("token", token);
        body.put("description", descripcion);
        body.put("installments", installments);
        body.put("payment_method_id", paymentMethodId);
        body.put("payer", payer);
        body.put("external_reference", externalReference);
        if (issuerId != null && !issuerId.isBlank()) {
            body.put("issuer_id", issuerId);
        }

        try {
            return restClient.post()
                    .uri("https://api.mercadopago.com/v1/payments")
                    .headers(h -> {
                        h.addAll(authHeaders());
                        h.set("X-Idempotency-Key", UUID.randomUUID().toString());
                    })
                    .body(body)
                    .retrieve()
                    .body(Map.class);
        } catch (RestClientResponseException e) {
            return Map.of("status", "error",
                    "error", "Mercado Pago respondió " + e.getStatusCode().value() + ": " + e.getResponseBodyAsString());
        } catch (RestClientException e) {
            return Map.of("status", "error", "error", "No se pudo comunicar con Mercado Pago");
        }
    }
}
