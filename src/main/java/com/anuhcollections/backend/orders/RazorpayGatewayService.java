package com.anuhcollections.backend.orders;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import tools.jackson.databind.JsonNode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RazorpayGatewayService {

    private final RestClient restClient;
    private final String keyId;
    private final String keySecret;

    public RazorpayGatewayService(
            RestClient.Builder restClientBuilder,
            @Value("${razorpay.key-id:}") String keyId,
            @Value("${razorpay.key-secret:}") String keySecret) {
        this.restClient = restClientBuilder
                .baseUrl("https://api.razorpay.com/v1")
                .build();
        this.keyId = keyId;
        this.keySecret = keySecret;
    }

    public String getKeyId() {
        requireConfigured();
        return keyId;
    }

    public GatewayOrder createOrder(long amountPaise, String receipt) {
        requireConfigured();

        try {
            JsonNode response = restClient.post()
                    .uri("/orders")
                    .headers(headers ->
                            headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of(
                            "amount", amountPaise,
                            "currency", "INR",
                            "receipt", receipt))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("id").isMissingNode()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Payment gateway returned an invalid order");
            }

            return new GatewayOrder(
                    response.path("id").asText(),
                    response.path("amount").asLong(),
                    response.path("currency").asText());
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not create the Razorpay order");
        }
    }

    public boolean verifySignature(
            String razorpayOrderId,
            String razorpayPaymentId,
            String suppliedSignature) {
        requireConfigured();

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(
                    keySecret.getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"));

            String signedData =
                    razorpayOrderId + "|" + razorpayPaymentId;

            String expectedSignature = HexFormat.of().formatHex(
                    mac.doFinal(
                            signedData.getBytes(StandardCharsets.UTF_8)));

            return MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.UTF_8),
                    suppliedSignature.toLowerCase()
                            .getBytes(StandardCharsets.UTF_8));
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Could not verify Razorpay signature", exception);
        }
    }

    public GatewayPayment fetchPayment(String paymentId) {
        requireConfigured();

        try {
            JsonNode response = restClient.get()
                    .uri("/payments/{paymentId}", paymentId)
                    .headers(headers ->
                            headers.setBasicAuth(keyId, keySecret))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Payment gateway returned no payment details");
            }

            return new GatewayPayment(
                    response.path("id").asText(),
                    response.path("order_id").asText(),
                    response.path("status").asText(),
                    response.path("amount").asLong(),
                    response.path("currency").asText(),
                    response.path("method").asText());
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not verify the Razorpay payment");
        }
    }

    public String requestFullRefund(String paymentId, long amountPaise) {
        requireConfigured();

        try {
            JsonNode response = restClient.post()
                    .uri("/payments/{paymentId}/refund", paymentId)
                    .headers(headers ->
                            headers.setBasicAuth(keyId, keySecret))
                    .body(Map.of("amount", amountPaise))
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null || response.path("id").isMissingNode()) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Payment gateway did not return a refund ID");
            }

            return response.path("id").asText();
        } catch (RestClientException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Could not request the Razorpay refund");
        }
    }

    private void requireConfigured() {
        if (keyId == null || keyId.isBlank()
                || keySecret == null || keySecret.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "Razorpay test keys are not configured");
        }
    }

    public record GatewayOrder(
            String id,
            long amount,
            String currency) {
    }

    public record GatewayPayment(
            String id,
            String orderId,
            String status,
            long amount,
            String currency,
            String method) {
    }
}