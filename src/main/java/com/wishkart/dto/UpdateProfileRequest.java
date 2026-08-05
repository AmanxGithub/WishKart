package com.wishkart.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for updating the current user's profile.
 */
@Data
public class UpdateProfileRequest {

    @Size(min = 1, max = 50, message = "First name must be between 1 and 50 characters")
    private String firstName;

    @Size(min = 1, max = 50, message = "Last name must be between 1 and 50 characters")
    private String lastName;

    @Size(max = 20, message = "Phone number is too long")
    private String phone;
}
