package com.anuhcollections.backend.orders;

import jakarta.validation.constraints.NotBlank;

public record PaymentVerificationRequest(
        @NotBlank String razorpayOrderId,
        @NotBlank String razorpayPaymentId,
        @NotBlank String razorpaySignature) {
}