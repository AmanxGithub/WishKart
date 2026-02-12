package com.wishkart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * WishKart E-Commerce Application
 * A full-featured e-commerce platform with user authentication,
 * payment processing, inventory management, and admin dashboard.
 */
@SpringBootApplication
@EnableAsync
@EnableScheduling
public class WishKartApplication {

    public static void main(String[] args) {
        SpringApplication.run(WishKartApplication.class, args);
    }
}
