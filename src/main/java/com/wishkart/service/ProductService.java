package com.wishkart.service;

import com.wishkart.dto.CreateProductRequest;
import com.wishkart.dto.ProductDTO;
import com.wishkart.entity.Category;
import com.wishkart.entity.Product;
import com.wishkart.entity.ProductImage;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.CategoryRepository;
import com.wishkart.repository.ProductImageRepository;
import com.wishkart.repository.ProductRepository;
import com.wishkart.util.SlugUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;

/**
 * Service for product management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductImageRepository productImageRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    @Transactional(readOnly = true)
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return ProductDTO.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public ProductDTO getProductBySlug(String slug) {
        String redisKey = "product:" + slug;
        // 1. Check Redis
        Product product = (Product) redisTemplate.opsForValue().get(redisKey);
        if (product != null) {
            System.out.println("CACHE HIT");
        } else {
            // 2. Get from DB
            product = productRepository.findBySlug(slug)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));


            // 3. Put in Redis
            redisTemplate.opsForValue().set(
                    redisKey,
                    product,
                    Duration.ofMinutes(10)
            );
        }

        return ProductDTO.fromEntity(product);
    }

    @Transactional(readOnly = true)
    public Product findById(Long id) {
        return productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getAllProductsAdmin(Pageable pageable) {
        return productRepository.findAll(pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));
        return productRepository.findByCategoryAndActiveTrue(category, pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByPriceRange(minPrice, maxPrice, pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getFeaturedProducts(Pageable pageable) {
        return productRepository.findFeaturedProducts(pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getNewArrivals(Pageable pageable) {
        return productRepository.findNewArrivals(pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<ProductDTO> getBestSellers(Pageable pageable) {
        return productRepository.findBestSellingProducts(pageable)
            .map(ProductDTO::fromEntity);
    }

    @Transactional
    public ProductDTO createProduct(CreateProductRequest request) {
        // Validate SKU uniqueness
        if (productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU '" + request.getSku() + "' already exists");
        }

        // Generate slug
        String slug = SlugUtil.generateSlug(request.getName());
        int counter = 1;
        while (productRepository.existsBySlug(slug)) {
            slug = SlugUtil.generateSlug(request.getName()) + "-" + counter++;
        }

        // Find category if specified
        Category category = null;
        if (request.getCategoryId() != null) {
            category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
        }

        Product product = Product.builder()
            .name(request.getName())
            .sku(request.getSku())
            .slug(slug)
            .description(request.getDescription())
            .shortDescription(request.getShortDescription())
            .price(request.getPrice())
            .compareAtPrice(request.getCompareAtPrice())
            .costPrice(request.getCostPrice())
            .stockQuantity(request.getStockQuantity())
            .lowStockThreshold(request.getLowStockThreshold() != null ? request.getLowStockThreshold() : 10)
            .trackInventory(request.isTrackInventory())
            .active(request.isActive())
            .featured(request.isFeatured())
            .weight(request.getWeight())
            .weightUnit(request.getWeightUnit())
            .category(category)
            .metaTitle(request.getMetaTitle())
            .metaDescription(request.getMetaDescription())
            .build();

        product = productRepository.save(product);

        // Add images
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            Product finalProduct = product;
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                ProductImage image = ProductImage.builder()
                    .product(finalProduct)
                    .imageUrl(request.getImageUrls().get(i))
                    .displayOrder(i)
                    .primary(i == 0)
                    .build();
                product.addImage(image);
            }
            product = productRepository.save(product);
        }

        log.info("Product created: {} (SKU: {})", product.getName(), product.getSku());
        return ProductDTO.fromEntity(product);
    }

    @Transactional
    public ProductDTO updateProduct(Long id, CreateProductRequest request) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Check SKU uniqueness if changed
        if (!product.getSku().equals(request.getSku()) && productRepository.existsBySku(request.getSku())) {
            throw new BadRequestException("Product with SKU '" + request.getSku() + "' already exists");
        }

        // Update slug if name changed
        if (!product.getName().equals(request.getName())) {
            String slug = SlugUtil.generateSlug(request.getName());
            int counter = 1;
            while (productRepository.existsBySlug(slug) && !slug.equals(product.getSlug())) {
                slug = SlugUtil.generateSlug(request.getName()) + "-" + counter++;
            }
            product.setSlug(slug);
        }

        // Update category
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            product.setCategory(category);
        } else {
            product.setCategory(null);
        }

        product.setName(request.getName());
        product.setSku(request.getSku());
        product.setDescription(request.getDescription());
        product.setShortDescription(request.getShortDescription());
        product.setPrice(request.getPrice());
        product.setCompareAtPrice(request.getCompareAtPrice());
        product.setCostPrice(request.getCostPrice());
        product.setStockQuantity(request.getStockQuantity());
        if (request.getLowStockThreshold() != null) {
            product.setLowStockThreshold(request.getLowStockThreshold());
        }
        product.setTrackInventory(request.isTrackInventory());
        product.setActive(request.isActive());
        product.setFeatured(request.isFeatured());
        product.setWeight(request.getWeight());
        product.setWeightUnit(request.getWeightUnit());
        product.setMetaTitle(request.getMetaTitle());
        product.setMetaDescription(request.getMetaDescription());

        product = productRepository.save(product);
        log.info("Product updated: {} (ID: {})", product.getName(), product.getId());
        return ProductDTO.fromEntity(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productRepository.delete(product);
        log.info("Product deleted: {} (ID: {})", product.getName(), id);
    }

    @Transactional
    public void updateStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        product.setStockQuantity(quantity);
        productRepository.save(product);
        log.info("Stock updated for product {}: {}", product.getSku(), quantity);
    }

    @Transactional
    public void decreaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        product.decreaseStock(quantity);
        productRepository.save(product);
    }

    @Transactional
    public void increaseStock(Long productId, int quantity) {
        Product product = productRepository.findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
        product.increaseStock(quantity);
        productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
            .map(ProductDTO::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public List<ProductDTO> getOutOfStockProducts() {
        return productRepository.findOutOfStockProducts().stream()
            .map(ProductDTO::fromEntity)
            .toList();
    }

    @Transactional(readOnly = true)
    public long countActiveProducts() {
        return productRepository.countActiveProducts();
    }

    @Transactional(readOnly = true)
    public long countLowStockProducts() {
        return productRepository.countLowStockProducts();
    }
}
