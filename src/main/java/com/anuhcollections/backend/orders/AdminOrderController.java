package com.anuhcollections.backend.orders;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    public AdminOrderController(AdminOrderService adminOrderService) {
        this.adminOrderService = adminOrderService;
    }

    @GetMapping
    public List<OrderResponse> getAllOrders() {
        return adminOrderService.getAllOrders();
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody AdminOrderStatusRequest request) {
        return adminOrderService.updateStatus(id, request);
    }
}