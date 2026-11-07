package com.anuhcollections.backend.wishlist;

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
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;

    public WishlistService(
            WishlistRepository wishlistRepository,
            UserRepository userRepository,
            ProductRepository productRepository) {
        this.wishlistRepository = wishlistRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
    }

    public WishlistResponse getWishlist(String email) {
        User user = getUser(email);

        return wishlistRepository.findByUser_Id(user.getId())
                .map(this::toResponse)
                .orElseGet(WishlistResponse::empty);
    }

    @Transactional
    public WishlistResponse addItem(
            String email,
            AddToWishlistRequest request) {
        User user = getUser(email);

        Product product = productRepository
                .findByIdAndActiveTrue(request.productId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active product not found"));

        Wishlist wishlist = wishlistRepository
                .findByUser_Id(user.getId())
                .orElseGet(() -> {
                    Wishlist newWishlist = new Wishlist();
                    newWishlist.setUser(user);
                    return newWishlist;
                });

        boolean alreadySaved = wishlist.getItems().stream()
                .anyMatch(item -> item.getProduct().getId()
                        .equals(product.getId()));

        if (!alreadySaved) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlist.getItems().add(item);
        }

        return toResponse(wishlistRepository.save(wishlist));
    }

    @Transactional
    public void removeItem(String email, Long productId) {
        User user = getUser(email);

        wishlistRepository.findByUser_Id(user.getId())
                .ifPresent(wishlist -> {
                    wishlist.getItems().removeIf(item ->
                            item.getProduct().getId().equals(productId));
                    wishlistRepository.save(wishlist);
                });
    }

    private User getUser(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Account not found"));
    }

    private WishlistResponse toResponse(Wishlist wishlist) {
        var responseItems = wishlist.getItems().stream()
                .map(item -> {
                    Product product = item.getProduct();

                    return new WishlistItemResponse(
                            product.getId(),
                            product.getName(),
                            product.getSlug(),
                            product.getImageUrl(),
                            product.getPrice(),
                            product.isActive()
                                    && product.getStockQuantity() > 0);
                })
                .toList();

        return new WishlistResponse(responseItems.size(), responseItems);
    }
}