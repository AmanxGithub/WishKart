package com.wishkart.service;

import com.wishkart.dto.UserDTO;
import com.wishkart.entity.User;
import com.wishkart.exception.BadRequestException;
import com.wishkart.exception.ResourceNotFoundException;
import com.wishkart.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for user management operations.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public UserDTO getUserById(Long id) {
        User user = userRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
        return UserDTO.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public UserDTO getUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return UserDTO.fromEntity(user);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getAllUsers(Pageable pageable) {
        return userRepository.findAll(pageable)
            .map(UserDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> searchUsers(String search, Pageable pageable) {
        return userRepository.searchUsers(search, pageable)
            .map(UserDTO::fromEntity);
    }

    @Transactional(readOnly = true)
    public Page<UserDTO> getUsersByRole(User.Role role, Pageable pageable) {
        return userRepository.findByRole(role, pageable)
            .map(UserDTO::fromEntity);
    }

    @Transactional
    public UserDTO updateProfile(Long userId, String firstName, String lastName, String phone) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (firstName != null && !firstName.isBlank()) {
            user.setFirstName(firstName);
        }
        if (lastName != null && !lastName.isBlank()) {
            user.setLastName(lastName);
        }
        if (phone != null) {
            user.setPhone(phone);
        }

        user = userRepository.save(user);
        log.info("Profile updated for user: {}", user.getEmail());
        return UserDTO.fromEntity(user);
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BadRequestException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        log.info("Password changed for user: {}", user.getEmail());
    }

    @Transactional
    public void toggleUserStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        log.info("User {} status changed to: {}", user.getEmail(), user.isEnabled() ? "enabled" : "disabled");
    }

    @Transactional
    public void changeUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setRole(newRole);
        userRepository.save(user);
        log.info("User {} role changed to: {}", user.getEmail(), newRole);
    }

    @Transactional(readOnly = true)
    public long countUsers() {
        return userRepository.count();
    }

    @Transactional(readOnly = true)
    public long countCustomers() {
        return userRepository.countByRole(User.Role.CUSTOMER);
    }

    @Transactional(readOnly = true)
    public long countAdmins() {
        return userRepository.countByRole(User.Role.ADMIN);
    }
}
