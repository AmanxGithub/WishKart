package com.wishkart.dto;

import com.wishkart.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDTO {

    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private String phone;
    private String role;
    private boolean enabled;
    private boolean emailVerified;
    private LocalDateTime createdAt;

    public static UserDTO fromEntity(User user) {
        return UserDTO.builder()
            .id(user.getId())
            .firstName(user.getFirstName())
            .lastName(user.getLastName())
            .email(user.getEmail())
            .phone(user.getPhone())
            .role(user.getRole().name())
            .enabled(user.isEnabled())
            .emailVerified(user.isEmailVerified())
            .createdAt(user.getCreatedAt())
            .build();
    }

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
