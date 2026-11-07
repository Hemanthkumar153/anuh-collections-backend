package com.anuhcollections.backend.orders;

public record OrderCheckoutResponse(
        OrderResponse order,
        RazorpayCheckout razorpayCheckout) {

    public record RazorpayCheckout(
            String keyId,
            String razorpayOrderId,
            long amount,
            String currency) {
    }
}