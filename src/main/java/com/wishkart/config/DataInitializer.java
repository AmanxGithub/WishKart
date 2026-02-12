package com.wishkart.config;

import com.wishkart.entity.*;
import com.wishkart.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Data initializer for creating default admin user and sample data.
 * Prices are in INR (Indian Rupees).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final CouponRepository couponRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${wishkart.admin.default-email:admin@wishkart.com}")
    private String adminEmail;

    @Value("${wishkart.admin.default-password:Admin@123}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        createDefaultAdmin();
        createSampleCategories();
        createSampleProducts();
        createSampleCoupons();
        log.info("============================================");
        log.info("Data initialization completed!");
        log.info("============================================");
        log.info("Admin Login: {} / {}", adminEmail, adminPassword);
        log.info("Customer Login: customer@wishkart.com / Customer@123");
        log.info("============================================");
    }

    private void createDefaultAdmin() {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .phone("+91 9876543210")
                .role(User.Role.ADMIN)
                .enabled(true)
                .emailVerified(true)
                .build();
            admin = userRepository.save(admin);

            // Create cart for admin
            Cart cart = Cart.builder().user(admin).build();
            cartRepository.save(cart);

            log.info("Default admin user created: {}", adminEmail);
        }

        // Create a sample customer
        if (userRepository.findByEmail("customer@wishkart.com").isEmpty()) {
            User customer = User.builder()
                .firstName("Rahul")
                .lastName("Sharma")
                .email("customer@wishkart.com")
                .password(passwordEncoder.encode("Customer@123"))
                .phone("+91 9988776655")
                .role(User.Role.CUSTOMER)
                .enabled(true)
                .emailVerified(true)
                .build();
            customer = userRepository.save(customer);

            Cart cart = Cart.builder().user(customer).build();
            cartRepository.save(cart);

            log.info("Sample customer created: customer@wishkart.com");
        }
    }

    private void createSampleCategories() {
        if (categoryRepository.count() == 0) {
            // Electronics
            Category electronics = Category.builder()
                .name("Electronics")
                .slug("electronics")
                .description("Latest gadgets, smartphones, laptops and more")
                .imageUrl("https://images.unsplash.com/photo-1498049794561-7780e7231661?w=400")
                .displayOrder(1)
                .active(true)
                .build();
            electronics = categoryRepository.save(electronics);

            categoryRepository.saveAll(Arrays.asList(
                Category.builder().name("Smartphones").slug("smartphones").parent(electronics).displayOrder(1).active(true).build(),
                Category.builder().name("Laptops").slug("laptops").parent(electronics).displayOrder(2).active(true).build(),
                Category.builder().name("Tablets").slug("tablets").parent(electronics).displayOrder(3).active(true).build(),
                Category.builder().name("Audio").slug("audio").parent(electronics).displayOrder(4).active(true).build()
            ));

            // Fashion
            Category fashion = Category.builder()
                .name("Fashion")
                .slug("fashion")
                .description("Trending clothes, footwear and accessories")
                .imageUrl("https://images.unsplash.com/photo-1445205170230-053b83016050?w=400")
                .displayOrder(2)
                .active(true)
                .build();
            fashion = categoryRepository.save(fashion);

            categoryRepository.saveAll(Arrays.asList(
                Category.builder().name("Men's Fashion").slug("mens-fashion").parent(fashion).displayOrder(1).active(true).build(),
                Category.builder().name("Women's Fashion").slug("womens-fashion").parent(fashion).displayOrder(2).active(true).build(),
                Category.builder().name("Kids' Fashion").slug("kids-fashion").parent(fashion).displayOrder(3).active(true).build()
            ));

            // Home & Living
            Category homeLiving = Category.builder()
                .name("Home & Living")
                .slug("home-living")
                .description("Furniture, decor and home essentials")
                .imageUrl("https://images.unsplash.com/photo-1484101403633-562f891dc89a?w=400")
                .displayOrder(3)
                .active(true)
                .build();
            categoryRepository.save(homeLiving);

            // Health & Beauty
            Category healthBeauty = Category.builder()
                .name("Health & Beauty")
                .slug("health-beauty")
                .description("Personal care, skincare and wellness products")
                .imageUrl("https://images.unsplash.com/photo-1596462502278-27bfdc403348?w=400")
                .displayOrder(4)
                .active(true)
                .build();
            categoryRepository.save(healthBeauty);

            // Sports & Outdoors
            Category sports = Category.builder()
                .name("Sports & Fitness")
                .slug("sports-fitness")
                .description("Sports equipment, fitness gear and outdoor accessories")
                .imageUrl("https://images.unsplash.com/photo-1517836357463-d25dfeac3438?w=400")
                .displayOrder(5)
                .active(true)
                .build();
            categoryRepository.save(sports);

            // Books
            Category books = Category.builder()
                .name("Books")
                .slug("books")
                .description("Bestsellers, fiction, non-fiction and more")
                .imageUrl("https://images.unsplash.com/photo-1495446815901-a7297e633e8d?w=400")
                .displayOrder(6)
                .active(true)
                .build();
            categoryRepository.save(books);

            log.info("Sample categories created");
        }
    }

    private void createSampleProducts() {
        if (productRepository.count() == 0) {
            Category electronics = categoryRepository.findBySlug("electronics").orElse(null);
            Category smartphones = categoryRepository.findBySlug("smartphones").orElse(null);
            Category laptops = categoryRepository.findBySlug("laptops").orElse(null);
            Category audio = categoryRepository.findBySlug("audio").orElse(null);
            Category fashion = categoryRepository.findBySlug("fashion").orElse(null);
            Category homeLiving = categoryRepository.findBySlug("home-living").orElse(null);

            List<Product> products = Arrays.asList(
                // Smartphones (INR prices)
                createProduct("iPhone 15 Pro Max 256GB", "iphone-15-pro-max", "SKU-IP15PM",
                    "The most powerful iPhone ever with A17 Pro chip, 48MP camera system, and titanium design. Features 5x optical zoom, Action button, and USB-C with USB 3 speeds.",
                    new BigDecimal("159900"), new BigDecimal("169900"), 50, smartphones, true,
                    "https://images.unsplash.com/photo-1695048133142-1a20484d2569?w=500", 4.8, 342),

                createProduct("Samsung Galaxy S24 Ultra", "samsung-galaxy-s24-ultra", "SKU-SGS24U",
                    "Ultimate Galaxy experience with Galaxy AI, 200MP camera, Titanium frame, and S Pen included. Snapdragon 8 Gen 3 processor.",
                    new BigDecimal("134999"), null, 45, smartphones, true,
                    "https://images.unsplash.com/photo-1610945265064-0e34e5519bbf?w=500", 4.7, 256),

                createProduct("OnePlus 12 5G", "oneplus-12-5g", "SKU-OP12",
                    "Flagship killer with Snapdragon 8 Gen 3, Hasselblad camera, 100W SUPERVOOC charging, and 2K 120Hz ProXDR display.",
                    new BigDecimal("64999"), new BigDecimal("69999"), 80, smartphones, true,
                    "https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=500", 4.6, 189),

                // Laptops
                createProduct("MacBook Pro 14\" M3 Pro", "macbook-pro-14-m3", "SKU-MBP14",
                    "Apple M3 Pro chip, 18GB unified memory, 512GB SSD, stunning Liquid Retina XDR display with ProMotion.",
                    new BigDecimal("199900"), new BigDecimal("219900"), 30, laptops, true,
                    "https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=500", 2, 128),

                createProduct("Dell XPS 15", "dell-xps-15", "SKU-DXPS15",
                    "Intel Core i7 13th Gen, 16GB RAM, 512GB SSD, NVIDIA RTX 4050, 15.6\" 3.5K OLED InfinityEdge display.",
                    new BigDecimal("149990"), new BigDecimal("169990"), 25, laptops, false,
                    "https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=500", 4.5, 87),

                createProduct("HP Pavilion Gaming 15", "hp-pavilion-gaming-15", "SKU-HPG15",
                    "AMD Ryzen 7, 16GB RAM, 512GB SSD, NVIDIA GTX 1650, 144Hz FHD display. Perfect for gaming and productivity.",
                    new BigDecimal("74990"), new BigDecimal("84990"), 60, laptops, false,
                    "https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=500", 4.4, 156),

                // Audio
                createProduct("Sony WH-1000XM5", "sony-wh-1000xm5", "SKU-SWXM5",
                    "Industry-leading noise canceling headphones with exceptional sound quality, 30-hour battery life, and multipoint connectivity.",
                    new BigDecimal("29990"), new BigDecimal("34990"), 100, audio, true,
                    "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=500", 4.8, 423),

                createProduct("Apple AirPods Pro 2", "airpods-pro-2", "SKU-APP2",
                    "Active Noise Cancellation, Adaptive Audio, Conversation Awareness, and USB-C charging case with precision finding.",
                    new BigDecimal("24900"), null, 150, audio, true,
                    "https://images.unsplash.com/photo-1600294037681-c80b4cb5b434?w=500", 4.7, 567),

                createProduct("JBL Flip 6 Speaker", "jbl-flip-6", "SKU-JBL6",
                    "Portable Bluetooth speaker with powerful sound, IP67 waterproof, 12-hour playtime, and PartyBoost feature.",
                    new BigDecimal("12999"), new BigDecimal("15999"), 200, audio, false,
                    "https://images.unsplash.com/photo-1608043152269-423dbba4e7e1?w=500", 4.5, 312),

                createProduct("boAt Rockerz 450", "boat-rockerz-450", "SKU-BR450",
                    "Wireless headphones with 40mm drivers, 15-hour playback, padded ear cushions, and dual connectivity mode.",
                    new BigDecimal("1499"), new BigDecimal("2990"), 300, audio, false,
                    "https://images.unsplash.com/photo-1583394838336-acd977736f90?w=500", 4.2, 1245),

                // Fashion
                createProduct("Premium Cotton T-Shirt Pack", "premium-cotton-tshirt-pack", "SKU-PCT01",
                    "Pack of 3 premium 100% organic cotton t-shirts. Comfortable fit, available in multiple colors. Perfect for everyday wear.",
                    new BigDecimal("1299"), new BigDecimal("1999"), 500, fashion, false,
                    "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=500", 4.4, 892),

                createProduct("Classic Denim Jacket", "classic-denim-jacket", "SKU-CDJ01",
                    "Timeless denim jacket with modern fit. Perfect for all seasons. Features button closure and multiple pockets.",
                    new BigDecimal("2499"), new BigDecimal("3499"), 150, fashion, true,
                    "https://images.unsplash.com/photo-1576995853123-5a10305d93c0?w=500", 4.6, 234),

                createProduct("Running Shoes Pro", "running-shoes-pro", "SKU-RSP01",
                    "Lightweight running shoes with responsive cushioning, breathable mesh upper, and durable rubber outsole.",
                    new BigDecimal("4999"), new BigDecimal("6999"), 200, fashion, true,
                    "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=500", 4.5, 456),

                // Home & Living
                createProduct("Minimalist Desk Lamp", "minimalist-desk-lamp", "SKU-MDL01",
                    "Modern LED desk lamp with adjustable brightness, touch controls, USB charging port, and flexible arm design.",
                    new BigDecimal("1999"), new BigDecimal("2999"), 100, homeLiving, false,
                    "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?w=500", 4.3, 178),

                createProduct("Ceramic Coffee Mug Set", "ceramic-coffee-mug-set", "SKU-CCMS01",
                    "Set of 4 premium ceramic mugs with modern minimalist design. Microwave and dishwasher safe, 350ml capacity each.",
                    new BigDecimal("899"), new BigDecimal("1499"), 300, homeLiving, false,
                    "https://images.unsplash.com/photo-1514228742587-6b1558fcca3d?w=500", 4.6, 234),

                // Low stock item
                createProduct("Limited Edition Smartwatch", "limited-edition-smartwatch", "SKU-LESW01",
                    "Exclusive smartwatch with titanium body, AMOLED display, advanced health monitoring, and 14-day battery life.",
                    new BigDecimal("24999"), new BigDecimal("34999"), 8, electronics, true,
                    "https://images.unsplash.com/photo-1546868871-7041f2a55e12?w=500", 4.9, 45),

                // Out of stock item
                createProduct("PlayStation 5 Console", "playstation-5-console", "SKU-PS5",
                    "Next-gen gaming console with 825GB SSD, ray tracing, 4K gaming at 120fps, and DualSense wireless controller.",
                    new BigDecimal("49990"), null, 0, electronics, false,
                    "https://images.unsplash.com/photo-1606813907291-d86efa9b94db?w=500", 4.8, 1023)
            );

            productRepository.saveAll(products);
            log.info("Sample products created: {} products", products.size());
        }
    }

    private Product createProduct(String name, String slug, String sku, String description,
                                  BigDecimal price, BigDecimal compareAtPrice, int stock,
                                  Category category, boolean featured, String imageUrl,
                                  double rating, int reviewCount) {
        Product product = Product.builder()
            .name(name)
            .slug(slug)
            .sku(sku)
            .description(description)
            .shortDescription(description.length() > 100 ? description.substring(0, 100) + "..." : description)
            .price(price)
            .compareAtPrice(compareAtPrice)
            .stockQuantity(stock)
            .lowStockThreshold(10)
            .trackInventory(true)
            .active(true)
            .featured(featured)
            .category(category)
            .averageRating(rating)
            .reviewCount(reviewCount)
            .build();

        // Add product image
        ProductImage image = ProductImage.builder()
            .product(product)
            .imageUrl(imageUrl)
            .altText(name)
            .displayOrder(0)
            .primary(true)
            .build();
        product.addImage(image);

        return product;
    }

    private void createSampleCoupons() {
        if (couponRepository.count() == 0) {
            List<Coupon> coupons = Arrays.asList(
                Coupon.builder()
                    .code("WELCOME10")
                    .description("Welcome offer - 10% off on your first order")
                    .discountType(Coupon.DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("10"))
                    .minimumOrderAmount(new BigDecimal("999"))
                    .maximumDiscount(new BigDecimal("500"))
                    .usageLimit(10000)
                    .perUserLimit(1)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusYears(1))
                    .active(true)
                    .build(),

                Coupon.builder()
                    .code("FLAT200")
                    .description("Flat ₹200 off on orders above ₹1,500")
                    .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                    .discountValue(new BigDecimal("200"))
                    .minimumOrderAmount(new BigDecimal("1500"))
                    .usageLimit(5000)
                    .perUserLimit(3)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(6))
                    .active(true)
                    .build(),

                Coupon.builder()
                    .code("BIGSALE20")
                    .description("Grand sale - 20% off (max ₹1,000) on orders above ₹3,000")
                    .discountType(Coupon.DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("20"))
                    .minimumOrderAmount(new BigDecimal("3000"))
                    .maximumDiscount(new BigDecimal("1000"))
                    .usageLimit(2000)
                    .perUserLimit(1)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(3))
                    .active(true)
                    .build(),

                Coupon.builder()
                    .code("FREESHIP")
                    .description("Free shipping - ₹99 off on any order")
                    .discountType(Coupon.DiscountType.FIXED_AMOUNT)
                    .discountValue(new BigDecimal("99"))
                    .minimumOrderAmount(BigDecimal.ZERO)
                    .usageLimit(10000)
                    .perUserLimit(5)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusYears(1))
                    .active(true)
                    .build(),

                Coupon.builder()
                    .code("ELECTRONICS15")
                    .description("15% off on Electronics (max ₹2,000)")
                    .discountType(Coupon.DiscountType.PERCENTAGE)
                    .discountValue(new BigDecimal("15"))
                    .minimumOrderAmount(new BigDecimal("5000"))
                    .maximumDiscount(new BigDecimal("2000"))
                    .usageLimit(1000)
                    .perUserLimit(2)
                    .startDate(LocalDateTime.now())
                    .endDate(LocalDateTime.now().plusMonths(2))
                    .active(true)
                    .build()
            );

            couponRepository.saveAll(coupons);
            log.info("Sample coupons created: {} coupons", coupons.size());
        }
    }
}
