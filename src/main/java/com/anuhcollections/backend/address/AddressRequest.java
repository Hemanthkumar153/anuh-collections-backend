package com.anuhcollections.backend.address;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AddressRequest(

        @NotBlank
        @Size(max = 120)
        String fullName,

        @NotBlank
        @Pattern(
                regexp = "^(?:\\+91[- ]?)?[6-9][0-9]{9}$",
                message = "Enter a valid Indian mobile number")
        String phone,

        @NotBlank
        @Size(max = 180)
        String addressLine1,

        @Size(max = 180)
        String addressLine2,

        @Size(max = 120)
        String landmark,

        @NotBlank
        @Size(max = 80)
        String city,

        @NotBlank
        @Size(max = 80)
        String state,

        @NotBlank
        @Pattern(
                regexp = "^[1-9][0-9]{5}$",
                message = "Enter a valid 6-digit Indian PIN code")
        String postalCode,

        boolean defaultAddress) {
}