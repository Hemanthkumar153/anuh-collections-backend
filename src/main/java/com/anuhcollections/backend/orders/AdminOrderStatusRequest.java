package com.anuhcollections.backend.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminOrderStatusRequest(
        @NotNull OrderStatus status,
        @Size(max = 80) String shippingCarrier,
        @Size(max = 100) String trackingNumber,
        @Size(max = 1000) String trackingUrl) {
}