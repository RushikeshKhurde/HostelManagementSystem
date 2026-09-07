package com.hostel.management.service;

import com.hostel.management.dto.AuthResponse;
import com.hostel.management.dto.LoginRequest;
import com.hostel.management.dto.ProfileUpdateRequest;
import com.hostel.management.dto.RegisterRequest;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.JwtUtil;
import com.hostel.management.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String email = request.getEmail().trim().toLowerCase();
        String mobile = request.getMobileNumber().trim();

        if (userRepository.existsByUsername(username)) {
            throw new ApiException("Username '" + username + "' is already taken", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email '" + email + "' is already registered", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByMobileNumber(mobile)) {
            throw new ApiException("Mobile number is already registered", HttpStatus.CONFLICT);
        }

        if (request.getConfirmPassword() != null && !request.getPassword().equals(request.getConfirmPassword())) {
            throw new ApiException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        if (request.getDateOfBirth() != null && request.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ApiException("Date of birth cannot be a future date", HttpStatus.BAD_REQUEST);
        }

        // Public registration ALWAYS assigns role USER. Even if role is provided, it is strictly ignored.
        Role role = Role.USER;

        User user = User.builder()
                .fullName(request.getFullName().trim())
                .username(username)
                .email(email)
                .mobileNumber(mobile)
                // Password is BCrypt hashed before persisting to database
                .password(passwordEncoder.encode(request.getPassword()))
                .role(role)
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .status("ACTIVE")
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        String token = jwtUtil.generateToken(savedUser.getUsername(), savedUser.getRole().name());
        return mapToAuthResponse(savedUser, token);
    }

    public AuthResponse login(LoginRequest request) {
        String identifier = request.getResolvedIdentifier();
        if (identifier.isEmpty()) {
            throw new ApiException("Username or email is required", HttpStatus.BAD_REQUEST);
        }

        User user = userRepository.findByUsernameOrEmail(identifier)
                .or(() -> userRepository.findByMobileNumber(identifier))
                .orElseThrow(() -> new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED));

        if (!user.isEnabled() || "INACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException("Account is deactivated. Contact administrator.", HttpStatus.FORBIDDEN);
        }

        // matches() verifies the raw password against the stored BCrypt hash safely
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new ApiException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        String token = jwtUtil.generateToken(user.getUsername(), user.getRole().name());
        return mapToAuthResponse(user, token);
    }

    public AuthResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new ApiException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        return mapToAuthResponse(user, null);
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String newEmail = req.getEmail().trim().toLowerCase();
        String newMobile = req.getMobileNumber().trim();

        // Check if email changed and taken by another user
        if (!user.getEmail().equalsIgnoreCase(newEmail) && userRepository.existsByEmail(newEmail)) {
            throw new ApiException("Email address '" + newEmail + "' is already in use by another account", HttpStatus.CONFLICT);
        }

        // Check if mobile changed and taken by another user
        if (!user.getMobileNumber().equals(newMobile) && userRepository.existsByMobileNumber(newMobile)) {
            throw new ApiException("Mobile number '" + newMobile + "' is already registered to another account", HttpStatus.CONFLICT);
        }

        if (req.getDateOfBirth() != null && req.getDateOfBirth().isAfter(LocalDate.now())) {
            throw new ApiException("Date of birth cannot be a future date", HttpStatus.BAD_REQUEST);
        }

        // Password change handling if requested
        if (req.getNewPassword() != null && !req.getNewPassword().isBlank()) {
            if (req.getCurrentPassword() == null || req.getCurrentPassword().isBlank()) {
                throw new ApiException("Current password is required to set a new password", HttpStatus.BAD_REQUEST);
            }
            if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPassword())) {
                throw new ApiException("Current password does not match our records", HttpStatus.BAD_REQUEST);
            }
            if (req.getNewPassword().length() < 6) {
                throw new ApiException("New password must be at least 6 characters long", HttpStatus.BAD_REQUEST);
            }
            user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        }

        // Update profile fields
        user.setFullName(req.getFullName().trim());
        user.setEmail(newEmail);
        user.setMobileNumber(newMobile);
        if (req.getGender() != null) user.setGender(req.getGender());
        if (req.getDateOfBirth() != null) user.setDateOfBirth(req.getDateOfBirth());
        if (req.getAddress() != null) user.setAddress(req.getAddress().trim());
        user.setUpdatedAt(LocalDateTime.now());

        User saved = userRepository.save(user);

        // Generate refreshed token
        String token = jwtUtil.generateToken(saved.getUsername(), saved.getRole().name());
        return mapToAuthResponse(saved, token);
    }

    private AuthResponse mapToAuthResponse(User user, String token) {
        return AuthResponse.builder()
                .token(token)
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .mobileNumber(user.getMobileNumber())
                .role(user.getRole().name())
                .gender(user.getGender())
                .dateOfBirth(user.getDateOfBirth())
                .address(user.getAddress())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
