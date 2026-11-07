package com.anuhcollections.backend.wishlist;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToWishlistRequest(

        @NotNull
        @Positive
        Long productId) {
}