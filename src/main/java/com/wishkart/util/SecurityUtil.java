package com.wishkart.util;

import com.wishkart.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * Utility class for security-related operations.
 */
public final class SecurityUtil {

    private SecurityUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Gets the currently authenticated user's ID.
     *
     * @return Optional containing user ID if authenticated
     */
    public static Optional<Long> getCurrentUserId() {
        return getCurrentUserDetails().map(CustomUserDetails::getId);
    }

    /**
     * Gets the currently authenticated user's email.
     *
     * @return Optional containing email if authenticated
     */
    public static Optional<String> getCurrentUserEmail() {
        return getCurrentUserDetails().map(CustomUserDetails::getEmail);
    }

    /**
     * Gets the current CustomUserDetails if authenticated.
     *
     * @return Optional containing CustomUserDetails if authenticated
     */
    public static Optional<CustomUserDetails> getCurrentUserDetails() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return Optional.empty();
        }

        Object principal = authentication.getPrincipal();
        if (principal instanceof CustomUserDetails) {
            return Optional.of((CustomUserDetails) principal);
        }

        return Optional.empty();
    }

    /**
     * Checks if the current user is authenticated.
     *
     * @return true if authenticated
     */
    public static boolean isAuthenticated() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null &&
               authentication.isAuthenticated() &&
               authentication.getPrincipal() instanceof CustomUserDetails;
    }

    /**
     * Checks if the current user has admin role.
     *
     * @return true if admin
     */
    public static boolean isAdmin() {
        return getCurrentUserDetails()
            .map(CustomUserDetails::isAdmin)
            .orElse(false);
    }
}
