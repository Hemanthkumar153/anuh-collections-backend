package com.anuhcollections.backend.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        String orderNumber,
        OrderStatus status,
        PaymentMethod paymentMethod,
        PaymentStatus paymentStatus,
        String gatewayPaymentMethod,
        BigDecimal subtotal,
        BigDecimal shippingFee,
        BigDecimal total,
        LocalDateTime createdAt,
        String shippingCarrier,
        String trackingNumber,
        String trackingUrl,
        ShippingAddress shippingAddress,
        List<OrderLine> items) {

    public OrderResponse {
        items = List.copyOf(items);
    }

    public record ShippingAddress(
            String fullName,
            String phone,
            String addressLine1,
            String addressLine2,
            String landmark,
            String city,
            String state,
            String postalCode,
            String country) {
    }

    public record OrderLine(
            Long productId,
            String sku,
            String productName,
            String productSlug,
            String imageUrl,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal lineTotal) {
    }
}