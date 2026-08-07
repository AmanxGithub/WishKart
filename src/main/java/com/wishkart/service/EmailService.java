package com.wishkart.service;

import com.wishkart.entity.Order;
import com.wishkart.entity.OrderItem;
import com.wishkart.entity.User;
import com.wishkart.event.OrderEvent;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.OrderRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Service for sending email notifications.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final OrderRepository orderRepository;

    @Value("${spring.mail.username:noreply@wishkart.com}")
    private String fromEmail;

    @Value("${wishkart.base-url:http://localhost:8085}")
    private String baseUrl;

    /**
     * Sends a welcome email to newly registered users.
     */
    @Async
    public void sendWelcomeEmail(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("email", user.getEmail());
        variables.put("loginUrl", baseUrl + "/auth/login");
        variables.put("shopUrl", baseUrl + "/products");

        sendEmail(
            user.getEmail(),
            "Welcome to WishKart! 🎉",
            "email/welcome",
            variables
        );
    }

    /**
     * Sends an order confirmation email.
     */
    @Async
    public void sendOrderConfirmationEmail(Order order) {
        User user = order.getUser();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("orderNumber", order.getOrderNumber());
        variables.put("orderDate", order.getCreatedAt().format(DateTimeFormatter.ofPattern("MMMM dd, yyyy")));
        variables.put("items", order.getItems());
        variables.put("subtotal", formatCurrency(order.getSubtotal()));
        variables.put("shipping", formatCurrency(order.getShippingCost()));
        variables.put("tax", formatCurrency(order.getTaxAmount()));
        variables.put("discount", formatCurrency(order.getDiscountAmount()));
        variables.put("total", formatCurrency(order.getTotalAmount()));
        variables.put("shippingAddress", formatAddress(order));
        variables.put("orderUrl", baseUrl + "/orders/" + order.getOrderNumber());

        sendEmail(
            user.getEmail(),
            "Order Confirmed: " + order.getOrderNumber(),
            "email/order-confirmation",
            variables
        );
    }

    /**
     * Sends an order shipped notification email.
     */
    @Async
    public void sendOrderShippedEmail(Order order) {
        User user = order.getUser();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("orderNumber", order.getOrderNumber());
        variables.put("trackingNumber", order.getTrackingNumber());
        variables.put("trackingUrl", "https://track.example.com/" + order.getTrackingNumber());
        variables.put("shippingAddress", formatAddress(order));
        variables.put("orderUrl", baseUrl + "/orders/" + order.getId());

        sendEmail(
            user.getEmail(),
            "Your Order Has Shipped! 📦 " + order.getOrderNumber(),
            "email/order-shipped",
            variables
        );
    }

    /**
     * Sends an order delivered notification email.
     */
    @Async
    public void sendOrderDeliveredEmail(Order order) {
        User user = order.getUser();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("orderNumber", order.getOrderNumber());
        variables.put("reviewUrl", baseUrl + "/orders/" + order.getId() + "/review");

        sendEmail(
            user.getEmail(),
            "Your Order Has Been Delivered! ✅ " + order.getOrderNumber(),
            "email/order-delivered",
            variables
        );
    }

    /**
     * Sends an order cancelled notification email.
     */
    @Async
    public void sendOrderCancelledEmail(Order order) {
        User user = order.getUser();

        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("orderNumber", order.getOrderNumber());
        variables.put("total", formatCurrency(order.getTotalAmount()));
        variables.put("contactUrl", baseUrl + "/contact");

        sendEmail(
            user.getEmail(),
            "Order Cancelled: " + order.getOrderNumber(),
            "email/order-cancelled",
            variables
        );
    }

    /**
     * Sends a password reset email.
     */
    @Async
    public void sendPasswordResetEmail(User user, String resetToken) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("resetUrl", baseUrl + "/auth/reset-password?token=" + resetToken);
        variables.put("expirationHours", 24);

        sendEmail(
            user.getEmail(),
            "Reset Your Password - WishKart",
            "email/password-reset",
            variables
        );
    }

    /**
     * Sends a password changed confirmation email.
     */
    @Async
    public void sendPasswordChangedEmail(User user) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("firstName", user.getFirstName());
        variables.put("contactUrl", baseUrl + "/contact");

        sendEmail(
            user.getEmail(),
            "Your Password Has Been Changed",
            "email/password-changed",
            variables
        );
    }

    /**
     * Sends a low stock alert to admin.
     */
    @Async
    public void sendLowStockAlert(String adminEmail, String productName, String sku, int currentStock) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("productName", productName);
        variables.put("sku", sku);
        variables.put("currentStock", currentStock);
        variables.put("adminUrl", baseUrl + "/admin/products");

        sendEmail(
            adminEmail,
            "⚠️ Low Stock Alert: " + productName,
            "email/low-stock-alert",
            variables
        );
    }

    /**
     * Sends a new order notification to admin.
     */
    @Async
    public void sendNewOrderNotification(String adminEmail, Order order) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("orderNumber", order.getOrderNumber());
        variables.put("customerName", order.getUser().getFirstName() + " " + order.getUser().getLastName());
        variables.put("total", formatCurrency(order.getTotalAmount()));
        variables.put("itemCount", order.getItems().size());
        variables.put("adminUrl", baseUrl + "/admin/orders/" + order.getId());

        sendEmail(
            adminEmail,
            "🛒 New Order Received: " + order.getOrderNumber(),
            "email/new-order-admin",
            variables
        );
    }

    /**
     * Sends order status update email.
     */
    @Async
    public void sendOrderStatusUpdateEmail(OrderEvent orderEvent) {
        Optional<Order> order = orderRepository.findByOrderNumber(orderEvent.getOrderNumber());
        order.ifPresent(order1 -> {
            order1.setUser(User.builder().firstName(orderEvent.getCustomerFirstName())
                    .email(orderEvent.getCustomerEmail()).build());
            order1.setItems(null);
            sendOrderConfirmationEmail(order1);
        });
    }

    /**
     * Core method to send an email using a template.
     */
    private void sendEmail(String to, String subject, String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);

            String htmlContent = templateEngine.process(templateName, context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Email sent successfully to {} with subject: {}", to, subject);

        } catch (MessagingException e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
            // In production, you might want to queue failed emails for retry
        } catch (Exception e) {
            log.error("Unexpected error sending email to {}: {}", to, e.getMessage());
        }
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "$0.00";
        }
        return String.format("$%.2f", amount);
    }

    private String formatAddress(Order order) {
        if (order.getShippingAddressLine1() == null) {
            return "N/A";
        }
        StringBuilder sb = new StringBuilder();
        if (order.getShippingName() != null) {
            sb.append(order.getShippingName()).append("\n");
        }
        sb.append(order.getShippingAddressLine1()).append("\n");
        if (order.getShippingAddressLine2() != null && !order.getShippingAddressLine2().isEmpty()) {
            sb.append(order.getShippingAddressLine2()).append("\n");
        }
        sb.append(order.getShippingCity()).append(", ")
          .append(order.getShippingState()).append(" ")
          .append(order.getShippingPostalCode()).append("\n");
        sb.append(order.getShippingCountry());
        return sb.toString();
    }
}
