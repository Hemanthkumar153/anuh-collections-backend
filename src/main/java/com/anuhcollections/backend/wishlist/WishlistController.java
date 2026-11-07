package com.anuhcollections.backend.wishlist;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public WishlistResponse getWishlist(Authentication authentication) {
        return wishlistService.getWishlist(authentication.getName());
    }

    @PostMapping("/items")
    public WishlistResponse addItem(
            Authentication authentication,
            @Valid @RequestBody AddToWishlistRequest request) {
        return wishlistService.addItem(
                authentication.getName(), request);
    }

    @DeleteMapping("/items/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeItem(
            Authentication authentication,
            @PathVariable Long productId) {
        wishlistService.removeItem(
                authentication.getName(), productId);
    }
}