package com.anuhcollections.backend.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProductAdminRequest(

        @NotBlank
        @Size(max = 50)
        String sku,

        @NotBlank
        @Size(max = 150)
        String name,

        @NotBlank
        @Size(max = 180)
        String slug,

        @Size(max = 2000)
        String description,

        @NotNull
        @DecimalMin(value = "0.01")
        @Digits(integer = 8, fraction = 2)
        BigDecimal price,

        @PositiveOrZero
        int stockQuantity,

        @Size(max = 1000)
        String imageUrl,

        @NotNull
        Long categoryId) {
}