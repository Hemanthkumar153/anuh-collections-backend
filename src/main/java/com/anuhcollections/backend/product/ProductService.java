package com.anuhcollections.backend.product;

import java.math.BigDecimal;

import com.anuhcollections.backend.category.Category;
import com.anuhcollections.backend.category.CategoryRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductResponse> searchProducts(
            String search,
            Long categoryId,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable) {

        return productRepository
                .findAll(
                        ProductSpecifications.filter(
                                search, categoryId, minPrice, maxPrice),
                        pageable)
                .map(this::toResponse);
    }

    public ProductResponse getActiveProduct(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Product not found"));

        return toResponse(product);
    }

    @Transactional
    public ProductResponse createProduct(ProductAdminRequest request) {
        if (productRepository.existsBySku(request.sku())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with this SKU already exists");
        }

        if (productRepository.existsBySlug(request.slug())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A product with this slug already exists");
        }

        Category category = getActiveCategory(request.categoryId());

        Product product = new Product();
        applyRequest(product, request, category);
        product.setActive(true);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            ProductAdminRequest request) {

        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active product not found"));

        if (productRepository.existsBySkuAndIdNot(
                request.sku(), productId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different product already uses this SKU");
        }

        if (productRepository.existsBySlugAndIdNot(
                request.slug(), productId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "A different product already uses this slug");
        }

        Category category = getActiveCategory(request.categoryId());

        applyRequest(product, request, category);

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void archiveProduct(Long productId) {
        Product product = productRepository.findByIdAndActiveTrue(productId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Active product not found"));

        product.setActive(false);
        productRepository.save(product);
    }

    private Category getActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .filter(Category::isActive)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Category was not found or is inactive"));

        return category;
    }

    private void applyRequest(
            Product product,
            ProductAdminRequest request,
            Category category) {

        product.setSku(request.sku().trim());
        product.setName(request.name().trim());
        product.setSlug(request.slug().trim());
        product.setDescription(request.description());
        product.setPrice(request.price());
        product.setStockQuantity(request.stockQuantity());
        product.setImageUrl(request.imageUrl());
        product.setCategory(category);
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getPrice(),
                product.getImageUrl(),
                product.getCategory().getName(),
                product.getCategory().getSlug(),
                product.getStockQuantity() > 0);
    }
}