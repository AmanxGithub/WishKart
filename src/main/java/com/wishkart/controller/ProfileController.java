package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.UserDTO;
import com.wishkart.service.UserService;
import com.wishkart.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for user profile endpoints.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "Profile", description = "User profile operations")
@SecurityRequirement(name = "bearerAuth")
public class ProfileController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "Get current user's profile")
    public ResponseEntity<ApiResponse<UserDTO>> getProfile() {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        UserDTO user = userService.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping
    @Operation(summary = "Update profile")
    public ResponseEntity<ApiResponse<UserDTO>> updateProfile(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) String phone) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        UserDTO user = userService.updateProfile(userId, firstName, lastName, phone);
        return ResponseEntity.ok(ApiResponse.success("Profile updated", user));
    }

    @PutMapping("/password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @RequestParam String currentPassword,
            @RequestParam String newPassword) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        userService.changePassword(userId, currentPassword, newPassword);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
