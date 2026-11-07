package com.anuhcollections.backend.auth;

public record LoginResponse(
        Long id,
        String fullName,
        String email,
        String role) {
}