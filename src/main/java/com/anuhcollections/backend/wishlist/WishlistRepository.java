package com.anuhcollections.backend.wishlist;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface WishlistRepository
        extends JpaRepository<Wishlist, Long> {

    Optional<Wishlist> findByUser_Id(Long userId);
}