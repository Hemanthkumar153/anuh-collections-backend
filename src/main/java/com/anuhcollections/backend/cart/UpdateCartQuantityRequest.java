package com.anuhcollections.backend.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record UpdateCartQuantityRequest(

        @Min(1)
        @Max(99)
        int quantity) {
}