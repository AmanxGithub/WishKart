package com.wishkart.controller;

import com.wishkart.dto.CategoryDTO;
import com.wishkart.dto.ProductDTO;
import com.wishkart.service.CategoryService;
import com.wishkart.service.ProductService;
import com.wishkart.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * Controller for web pages using Thymeleaf templates.
 */
@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductService productService;
    private final CategoryService categoryService;

    @GetMapping({"/", "/index", "/home"})
    public String home(Model model) {
        Page<ProductDTO> featuredProducts = productService.getFeaturedProducts(PageRequest.of(0, 8));
        Page<ProductDTO> newArrivals = productService.getNewArrivals(PageRequest.of(0, 8));
        List<CategoryDTO> categories = categoryService.getRootCategories();

        model.addAttribute("featuredProducts", featuredProducts.getContent());
        model.addAttribute("newArrivals", newArrivals.getContent());
        model.addAttribute("categories", categories);
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());
        model.addAttribute("isAdmin", SecurityUtil.isAdmin());

        SecurityUtil.getCurrentUserDetails().ifPresent(user ->
            model.addAttribute("currentUser", user)
        );

        return "index";
    }

    @GetMapping("/products")
    public String products(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category", required = false) Long category,
            Model model) {

        Page<ProductDTO> productPage;

        if (q != null && !q.isBlank()) {
            productPage = productService.searchProducts(q, PageRequest.of(page, size));
            model.addAttribute("searchQuery", q);
        } else if (category != null) {
            productPage = productService.getProductsByCategory(category, PageRequest.of(page, size));
            CategoryDTO categoryDTO = categoryService.getCategoryById(category);
            model.addAttribute("selectedCategory", categoryDTO);
        } else {
            productPage = productService.getAllProducts(PageRequest.of(page, size));
        }

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("totalItems", productPage.getTotalElements());
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());

        return "product/list";
    }

    @GetMapping("/products/{slug}")
    public String productDetail(@PathVariable("slug") String slug, Model model) {
        ProductDTO product = productService.getProductBySlug(slug);
        model.addAttribute("product", product);
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());

        // Related products from same category
        if (product.getCategoryId() != null) {
            Page<ProductDTO> relatedProducts = productService.getProductsByCategory(
                product.getCategoryId(), PageRequest.of(0, 4));
            model.addAttribute("relatedProducts", relatedProducts.getContent().stream()
                .filter(p -> !p.getId().equals(product.getId()))
                .limit(4)
                .toList());
        }

        return "product/detail";
    }

    @GetMapping("/categories/{slug}")
    public String categoryPage(
            @PathVariable("slug") String slug,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "12") int size,
            Model model) {

        CategoryDTO category = categoryService.getCategoryBySlug(slug);
        Page<ProductDTO> products = productService.getProductsByCategory(category.getId(), PageRequest.of(page, size));

        model.addAttribute("category", category);
        model.addAttribute("products", products.getContent());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", products.getTotalPages());
        model.addAttribute("categories", categoryService.getRootCategories());
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());

        return "product/list";
    }

    @GetMapping("/auth/login")
    public String login(Model model) {
        if (SecurityUtil.isAuthenticated()) {
            return "redirect:/";
        }
        return "auth/login";
    }

    @GetMapping("/auth/register")
    public String register(Model model) {
        if (SecurityUtil.isAuthenticated()) {
            return "redirect:/";
        }
        return "auth/register";
    }

    @GetMapping("/cart")
    public String cart(Model model) {
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());
        return "cart/cart";
    }

    @GetMapping("/checkout")
    public String checkout(Model model) {
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());
        return "checkout/checkout";
    }

    @GetMapping("/admin")
    public String adminDashboard(Model model) {
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());
        model.addAttribute("isAdmin", SecurityUtil.isAdmin());
        return "admin/dashboard";
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());
        model.addAttribute("isAdmin", SecurityUtil.isAdmin());
        SecurityUtil.getCurrentUserDetails().ifPresent(user ->
            model.addAttribute("currentUser", user)
        );
        return "profile/profile";
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        model.addAttribute("isAuthenticated", SecurityUtil.isAuthenticated());
        return "orders/orders";
    }
}
