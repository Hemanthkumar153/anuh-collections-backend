package com.anuhcollections.backend.product;

import java.math.BigDecimal;

public record ProductResponse(
        Long id,
        String sku,
        String name,
        String slug,
        String description,
        BigDecimal price,
        String imageUrl,
        String categoryName,
        String categorySlug,
        boolean inStock) {
}