package com.anuhcollections.backend.auth;

public record RegisterResponse(
        Long id,
        String fullName,
        String email,
        String role) {
}