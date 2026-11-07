package com.anuhcollections.backend.cart;

import java.math.BigDecimal;
import java.util.Optional;

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
public class CartService {

    private final CartRepository cartRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public CartService(
            CartRepository cartRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public CartResponse getCart(String email) {
        User user = getUser(email);

        return cartRepository.findByUser_Id(user.getId())
                .map(this::toResponse)
                .orElseGet(CartResponse::empty);
    }

    @Transactional
    public CartResponse addItem(String email, AddToCartRequest request) {
        User user = getUser(email);

        Product product = productRepository
                .findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active product not found"));

        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Cart newCart = new Cart();
                    newCart.setUser(user);
                    return newCart;
                });

        Optional<CartItem> existingItem = cart.getItems().stream()
                .filter(item -> item.getProduct().getId()
                        .equals(product.getId()))
                .findFirst();

        int newQuantity = request.quantity();

        if (existingItem.isPresent()) {
            newQuantity += existingItem.get().getQuantity();
        }

        checkStock(product, newQuantity);

        if (existingItem.isPresent()) {
            existingItem.get().setQuantity(newQuantity);
        } else {
            CartItem item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setQuantity(newQuantity);
            cart.getItems().add(item);
        }

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateQuantity(
            String email,
            Long productId,
            UpdateCartQuantityRequest request) {

        User user = getUser(email);

        Cart cart = cartRepository.findByUser_Id(user.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cart not found"));

        CartItem item = cart.getItems().stream()
                .filter(cartItem -> cartItem.getProduct().getId()
                        .equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product is not in the cart"));

        Product product = item.getProduct();

        if (!product.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This product is no longer available");
        }

        checkStock(product, request.quantity());
        item.setQuantity(request.quantity());

        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public void removeItem(String email, Long productId) {
        User user = getUser(email);

        cartRepository.findByUser_Id(user.getId()).ifPresent(cart -> {
            cart.getItems().removeIf(item ->
                    item.getProduct().getId().equals(productId));
            cartRepository.save(cart);
        });
    }

    @Transactional
    public void clearCart(String email) {
        User user = getUser(email);

        cartRepository.findByUser_Id(user.getId()).ifPresent(cart -> {
            cart.getItems().clear();
            cartRepository.save(cart);
        });
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account not found"));
    }

    private void checkStock(Product product, int requestedQuantity) {
        if (requestedQuantity > 99) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Cart quantity cannot exceed 99");
        }

        if (requestedQuantity > product.getStockQuantity()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Requested quantity exceeds available stock");
        }
    }

    private CartResponse toResponse(Cart cart) {
        var responseItems = cart.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();
                    BigDecimal lineTotal = product.getPrice()
                            .multiply(BigDecimal.valueOf(item.getQuantity()));

                    boolean available = product.isActive()
                            && product.getStockQuantity() >= item.getQuantity();

                    return new CartItemResponse(
                            product.getId(),
                            product.getName(),
                            product.getSlug(),
                            product.getImageUrl(),
                            product.getPrice(),
                            item.getQuantity(),
                            lineTotal,
                            available);
                })
                .toList();

        int totalQuantity = responseItems.stream()
                .mapToInt(CartItemResponse::quantity)
                .sum();

        BigDecimal subtotal = responseItems.stream()
                .map(CartItemResponse::lineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CartResponse(totalQuantity, subtotal, responseItems);
    }
}