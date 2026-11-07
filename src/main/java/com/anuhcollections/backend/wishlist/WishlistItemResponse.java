package com.anuhcollections.backend.wishlist;

import java.math.BigDecimal;

public record WishlistItemResponse(
        Long productId,
        String productName,
        String productSlug,
        String imageUrl,
        BigDecimal price,
        boolean available) {
}