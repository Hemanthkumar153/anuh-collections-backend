package com.anuhcollections.backend.orders;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderCheckoutResponse placeOrder(
            Authentication authentication,
            @Valid @RequestBody PlaceOrderRequest request) {
        return orderService.placeOrder(
                authentication.getName(), request);
    }

    @PostMapping("/{id}/payments/verify")
    public OrderResponse verifyOnlinePayment(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PaymentVerificationRequest request) {
        return orderService.verifyOnlinePayment(
                authentication.getName(), id, request);
    }

    @GetMapping
    public List<OrderResponse> getOrderHistory(
            Authentication authentication) {
        return orderService.getOrderHistory(authentication.getName());
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(
            Authentication authentication,
            @PathVariable Long id) {
        return orderService.getOrder(authentication.getName(), id);
    }
}