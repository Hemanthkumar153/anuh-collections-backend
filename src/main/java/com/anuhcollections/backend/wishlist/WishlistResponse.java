package com.anuhcollections.backend.wishlist;

import java.util.List;

public record WishlistResponse(
        int itemCount,
        List<WishlistItemResponse> items) {

    public WishlistResponse {
        items = List.copyOf(items);
    }

    public static WishlistResponse empty() {
        return new WishlistResponse(0, List.of());
    }
}