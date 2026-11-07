package com.anuhcollections.backend.orders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.anuhcollections.backend.address.Address;
import com.anuhcollections.backend.address.AddressRepository;
import com.anuhcollections.backend.cart.Cart;
import com.anuhcollections.backend.cart.CartItem;
import com.anuhcollections.backend.cart.CartRepository;
import com.anuhcollections.backend.product.Product;
import com.anuhcollections.backend.product.ProductRepository;
import com.anuhcollections.backend.user.User;
import com.anuhcollections.backend.user.UserRepository;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class OrderService {

    private static final BigDecimal SHIPPING_FEE = BigDecimal.ZERO;

    private final CustomerOrderRepository orderRepository;
    private final CartRepository cartRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final RazorpayGatewayService razorpayGatewayService;

    public OrderService(
            CustomerOrderRepository orderRepository,
            CartRepository cartRepository,
            AddressRepository addressRepository,
            UserRepository userRepository,
            ProductRepository productRepository,
            RazorpayGatewayService razorpayGatewayService) {
        this.orderRepository = orderRepository;
        this.cartRepository = cartRepository;
        this.addressRepository = addressRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.razorpayGatewayService = razorpayGatewayService;
    }

    @Transactional
    public OrderCheckoutResponse placeOrder(
            String email,
            PlaceOrderRequest request) {
        User user = getUser(email);

        Address address = addressRepository
                .findByIdAndUser_Id(request.addressId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Delivery address not found"));

        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Your cart is empty"));

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Your cart is empty");
        }

        CustomerOrder order = new CustomerOrder();
        order.setOrderNumber(createOrderNumber());
        order.setUser(user);
        order.setPaymentMethod(request.paymentMethod());
        order.setPaymentStatus(PaymentStatus.PENDING);
        order.setCreatedAt(LocalDateTime.now());
        copyAddress(address, order);

        BigDecimal subtotal = BigDecimal.ZERO;
        List<PreparedLine> lines = new ArrayList<>();

        for (CartItem cartItem : cart.getItems()) {
            Long productId = cartItem.getProduct().getId();

            Product product = productRepository
                    .findActiveProductForUpdate(productId)
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "A product in your cart is no longer available"));

            int quantity = cartItem.getQuantity();

            if (quantity > product.getStockQuantity()) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "A product in your cart no longer has enough stock");
            }

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(quantity));

            CustomerOrderItem orderItem = new CustomerOrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setSku(product.getSku());
            orderItem.setProductName(product.getName());
            orderItem.setProductSlug(product.getSlug());
            orderItem.setImageUrl(product.getImageUrl());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(quantity);
            orderItem.setLineTotal(lineTotal);
            order.getItems().add(orderItem);

            lines.add(new PreparedLine(product, quantity));
            subtotal = subtotal.add(lineTotal);
        }

        order.setSubtotal(subtotal);
        order.setShippingFee(SHIPPING_FEE);
        order.setTotal(subtotal.add(SHIPPING_FEE));

        if (request.paymentMethod() == PaymentMethod.COD) {
            order.setStatus(OrderStatus.PLACED);

            for (PreparedLine line : lines) {
                line.product().setStockQuantity(
                        line.product().getStockQuantity()
                                - line.quantity());
            }

            CustomerOrder savedOrder = orderRepository.save(order);
            removePurchasedItemsFromCart(cart, lines);
            cartRepository.save(cart);

            return new OrderCheckoutResponse(
                    toResponse(savedOrder), null);
        }

        order.setStatus(OrderStatus.PAYMENT_PENDING);
        CustomerOrder savedOrder = orderRepository.save(order);

        long amountPaise = toPaise(savedOrder.getTotal());

        RazorpayGatewayService.GatewayOrder gatewayOrder =
                razorpayGatewayService.createOrder(
                        amountPaise, savedOrder.getOrderNumber());

        savedOrder.setGatewayOrderId(gatewayOrder.id());
        savedOrder = orderRepository.save(savedOrder);

        var checkout = new OrderCheckoutResponse.RazorpayCheckout(
                razorpayGatewayService.getKeyId(),
                gatewayOrder.id(),
                gatewayOrder.amount(),
                gatewayOrder.currency());

        return new OrderCheckoutResponse(
                toResponse(savedOrder), checkout);
    }

    @Transactional
    public OrderResponse verifyOnlinePayment(
            String email,
            Long orderId,
            PaymentVerificationRequest request) {
        User user = getUser(email);

        CustomerOrder order = orderRepository
                .findOwnedOrderForUpdate(orderId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.PAID
                && request.razorpayPaymentId()
                        .equals(order.getGatewayPaymentId())) {
            return toResponse(order);
        }

        if (order.getPaymentMethod() != PaymentMethod.ONLINE
                || order.getStatus() != OrderStatus.PAYMENT_PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This order is not waiting for online payment");
        }

        if (!request.razorpayOrderId().equals(order.getGatewayOrderId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Razorpay order does not match this order");
        }

        boolean validSignature =
                razorpayGatewayService.verifySignature(
                        request.razorpayOrderId(),
                        request.razorpayPaymentId(),
                        request.razorpaySignature());

        if (!validSignature) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Payment signature is invalid");
        }

        RazorpayGatewayService.GatewayPayment payment =
                razorpayGatewayService.fetchPayment(
                        request.razorpayPaymentId());

        long expectedAmount = toPaise(order.getTotal());

        if (!"captured".equalsIgnoreCase(payment.status())
                || !order.getGatewayOrderId().equals(payment.orderId())
                || payment.amount() != expectedAmount
                || !"INR".equalsIgnoreCase(payment.currency())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Payment is not captured for the correct order amount");
        }

        order.setGatewayPaymentId(payment.id());
        order.setGatewayPaymentMethod(payment.method());

        List<PreparedLine> lines = new ArrayList<>();
        boolean stockStillAvailable = true;

        for (CustomerOrderItem orderItem : order.getItems()) {
            Long productId = orderItem.getProduct().getId();

            Product product = productRepository
                    .findActiveProductForUpdate(productId)
                    .orElse(null);

            if (product == null
                    || product.getStockQuantity() < orderItem.getQuantity()) {
                stockStillAvailable = false;
                break;
            }

            lines.add(new PreparedLine(product, orderItem.getQuantity()));
        }

        if (!stockStillAvailable) {
            String refundId = razorpayGatewayService.requestFullRefund(
                    payment.id(), expectedAmount);
            order.setGatewayRefundId(refundId);
            order.setStatus(OrderStatus.CANCELLED);
            order.setPaymentStatus(PaymentStatus.REFUND_PENDING);
            return toResponse(orderRepository.save(order));
        }

        for (PreparedLine line : lines) {
            line.product().setStockQuantity(
                    line.product().getStockQuantity()
                            - line.quantity());
        }

        order.setStatus(OrderStatus.PLACED);
        order.setPaymentStatus(PaymentStatus.PAID);

        CustomerOrder savedOrder = orderRepository.save(order);

        cartRepository.findByUser_Id(user.getId()).ifPresent(cart -> {
            removePurchasedItemsFromCart(cart, lines);
            cartRepository.save(cart);
        });

        return toResponse(savedOrder);
    }

    public List<OrderResponse> getOrderHistory(String email) {
        User user = getUser(email);

        return orderRepository
                .findAllByUser_IdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public OrderResponse getOrder(String email, Long orderId) {
        User user = getUser(email);

        CustomerOrder order = orderRepository
                .findByIdAndUser_Id(orderId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Order not found"));

        return toResponse(order);
    }

    private void removePurchasedItemsFromCart(
            Cart cart,
            List<PreparedLine> purchasedLines) {
        for (PreparedLine line : purchasedLines) {
            cart.getItems().removeIf(cartItem ->
                    cartItem.getProduct().getId()
                            .equals(line.product().getId()));
        }
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account not found"));
    }

    private long toPaise(BigDecimal rupees) {
        try {
            return rupees.movePointRight(2).longValueExact();
        } catch (ArithmeticException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Order total cannot be represented in paise");
        }
    }

    private String createOrderNumber() {
        String suffix = UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 12)
                .toUpperCase();

        return "AC-" + suffix;
    }

    private void copyAddress(Address address, CustomerOrder order) {
        order.setShippingName(address.getFullName());
        order.setShippingPhone(address.getPhone());
        order.setShippingAddressLine1(address.getAddressLine1());
        order.setShippingAddressLine2(address.getAddressLine2());
        order.setShippingLandmark(address.getLandmark());
        order.setShippingCity(address.getCity());
        order.setShippingState(address.getState());
        order.setShippingPostalCode(address.getPostalCode());
        order.setShippingCountry(address.getCountry());
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

    private record PreparedLine(Product product, int quantity) {
    }
}