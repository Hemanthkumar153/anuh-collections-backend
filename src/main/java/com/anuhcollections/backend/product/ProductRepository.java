package com.anuhcollections.backend.product;

import java.util.Optional;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository
        extends JpaRepository<Product, Long>,
                JpaSpecificationExecutor<Product> {

    Optional<Product> findByIdAndActiveTrue(Long id);

    boolean existsBySku(String sku);

    boolean existsBySlug(String slug);

    boolean existsBySkuAndIdNot(String sku, Long id);

    boolean existsBySlugAndIdNot(String slug, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select p
            from Product p
            where p.id = :productId
              and p.active = true
            """)
    Optional<Product> findActiveProductForUpdate(
            @Param("productId") Long productId);
}