package com.anuhcollections.backend.orders;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PlaceOrderRequest(
        @NotNull @Positive Long addressId,
        @NotNull PaymentMethod paymentMethod) {
}