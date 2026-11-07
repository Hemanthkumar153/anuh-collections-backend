package com.anuhcollections.backend.cart;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(
        int totalQuantity,
        BigDecimal subtotal,
        List<CartItemResponse> items) {

    public CartResponse {
        items = List.copyOf(items);
    }

    public static CartResponse empty() {
        return new CartResponse(0, BigDecimal.ZERO, List.of());
    }
}