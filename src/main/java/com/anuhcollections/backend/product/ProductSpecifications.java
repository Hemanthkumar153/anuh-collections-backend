package com.anuhcollections.backend.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Predicate;

public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    public static Specification<Product> filter(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice) {

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(criteriaBuilder.isTrue(root.get("active")));

            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";

                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("name")), pattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("description")), pattern),
                        criteriaBuilder.like(
                                criteriaBuilder.lower(root.get("sku")), pattern)));
            }

            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(
                        root.get("category").get("id"), categoryId));
            }

            if (minPrice != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"), minPrice));
            }

            if (maxPrice != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"), maxPrice));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}