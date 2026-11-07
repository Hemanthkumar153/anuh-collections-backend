package com.anuhcollections.backend.orders;

import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class AdminOrderService {

    private final CustomerOrderRepository orderRepository;

    public AdminOrderService(CustomerOrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public List<OrderResponse> getAllOrders() {
        return orderRepository
                .findAll(Sort.by(Sort.Direction.DESC, "createdAt"))
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse updateStatus(
            Long orderId,
            AdminOrderStatusRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        OrderStatus requestedStatus = request.status();

        if (requestedStatus == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order status is required");
        }

        boolean hasCarrier = hasText(request.shippingCarrier());
        boolean hasTrackingNumber = hasText(request.trackingNumber());
        boolean hasTrackingUrl = hasText(request.trackingUrl());
        boolean hasAnyTrackingDetails =
                hasCarrier || hasTrackingNumber || hasTrackingUrl;

        if (requestedStatus != OrderStatus.SHIPPED && hasAnyTrackingDetails) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Tracking details can only be saved when the order status is SHIPPED");
        }

        if (requestedStatus == OrderStatus.SHIPPED && hasAnyTrackingDetails) {
            if (!hasCarrier || !hasTrackingNumber) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Enter both a courier name and a tracking number");
            }

            if (hasText(request.trackingUrl())
                    && !isValidTrackingUrl(request.trackingUrl())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tracking link must be a valid HTTP or HTTPS URL");
            }
        }

        if (requestedStatus == order.getStatus()) {
            if (requestedStatus == OrderStatus.SHIPPED && hasAnyTrackingDetails) {
                saveTrackingDetails(order, request);
                return toResponse(orderRepository.save(order));
            }

            return toResponse(order);
        }

        boolean validTransition = switch (order.getStatus()) {
            case PLACED -> requestedStatus == OrderStatus.PROCESSING;
            case PROCESSING -> requestedStatus == OrderStatus.SHIPPED;
            case SHIPPED -> requestedStatus == OrderStatus.DELIVERED;
            default -> false;
        };

        if (!validTransition) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order cannot move from "
                            + order.getStatus()
                            + " to "
                            + requestedStatus);
        }

        if (order.getPaymentMethod() == PaymentMethod.ONLINE
                && order.getPaymentStatus() != PaymentStatus.PAID) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "An online order cannot be processed until payment is verified");
        }

        if (requestedStatus == OrderStatus.SHIPPED) {
            if (!hasCarrier || !hasTrackingNumber) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Enter a courier name and tracking number before marking the order as shipped");
            }

            if (hasText(request.trackingUrl())
                    && !isValidTrackingUrl(request.trackingUrl())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Tracking link must be a valid HTTP or HTTPS URL");
            }

            saveTrackingDetails(order, request);
        }

        order.setStatus(requestedStatus);

        CustomerOrder savedOrder = orderRepository.save(order);
        return toResponse(savedOrder);
    }

    private void saveTrackingDetails(
            CustomerOrder order,
            AdminOrderStatusRequest request) {
        order.setShippingCarrier(request.shippingCarrier().trim());
        order.setTrackingNumber(request.trackingNumber().trim());
        order.setTrackingUrl(
                hasText(request.trackingUrl())
                        ? request.trackingUrl().trim()
                        : null);
    }

    private boolean isValidTrackingUrl(String value) {
        try {
            URI uri = URI.create(value.trim());
            String scheme = uri.getScheme();

            return uri.isAbsolute()
                    && uri.getHost() != null
                    && scheme != null
                    && (scheme.equalsIgnoreCase("http")
                            || scheme.equalsIgnoreCase("https"));
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private OrderResponse toResponse(CustomerOrder order) {
        var shippingAddress = new OrderResponse.ShippingAddress(
                order.getShippingName(),
                order.getShippingPhone(),
                order.getShippingAddressLine1(),
                order.getShippingAddressLine2(),
                order.getShippingLandmark(),
                order.getShippingCity(),
                order.getShippingState(),
                order.getShippingPostalCode(),
                order.getShippingCountry());

        var items = order.getItems().stream()
                .map(item -> new OrderResponse.OrderLine(
                        item.getProduct() == null
                                ? null
                                : item.getProduct().getId(),
                        item.getSku(),
                        item.getProductName(),
                        item.getProductSlug(),
                        item.getImageUrl(),
                        item.getUnitPrice(),
                        item.getQuantity(),
                        item.getLineTotal()))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                order.getPaymentMethod(),
                order.getPaymentStatus(),
                order.getGatewayPaymentMethod(),
                order.getSubtotal(),
                order.getShippingFee(),
                order.getTotal(),
                order.getCreatedAt(),
                order.getShippingCarrier(),
                order.getTrackingNumber(),
                order.getTrackingUrl(),
                shippingAddress,
                items);
    }
}