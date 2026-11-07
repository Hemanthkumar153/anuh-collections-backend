package com.anuhcollections.backend.address;

public record AddressResponse(
        Long id,
        String fullName,
        String phone,
        String addressLine1,
        String addressLine2,
        String landmark,
        String city,
        String state,
        String postalCode,
        String country,
        boolean defaultAddress) {
}