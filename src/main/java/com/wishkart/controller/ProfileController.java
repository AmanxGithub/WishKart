package com.wishkart.controller;

import com.wishkart.dto.ApiResponse;
import com.wishkart.dto.ChangePasswordRequest;
import com.wishkart.dto.UpdateProfileRequest;
import com.wishkart.dto.UserDTO;
import com.wishkart.service.UserService;
import com.wishkart.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
            @Valid @RequestBody UpdateProfileRequest request) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        UserDTO user = userService.updateProfile(
            userId, request.getFirstName(), request.getLastName(), request.getPhone());
        return ResponseEntity.ok(ApiResponse.success("Profile updated", user));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = SecurityUtil.getCurrentUserId()
            .orElseThrow(() -> new RuntimeException("User not authenticated"));
        userService.changePassword(userId, request.getCurrentPassword(), request.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
