package com.hostel.management.service;

import com.hostel.management.dto.AuthResponse;
import com.hostel.management.dto.LoginRequest;
import com.hostel.management.dto.ProfileUpdateRequest;
import com.hostel.management.dto.RegisterRequest;
import com.hostel.management.dto.ResetPasswordRequest;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.OtpVerification;
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
    private final StudentIdGeneratorService studentIdGeneratorService;
    private final OtpService otpService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String username = request.getUsername().trim();
        String rawEmail = request.getEmail();
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }
        if (rawEmail.chars().anyMatch(Character::isUpperCase)) {
            throw new ApiException("Email must be in lowercase.", HttpStatus.BAD_REQUEST);
        }
        String email = rawEmail.trim().toLowerCase();
        if (!email.matches("^[a-z0-9._%+-]+@([a-z0-9-]+\\.)+[a-z]{2,}$")) {
            throw new ApiException("Please enter a valid email address (e.g., username@domain.com).", HttpStatus.BAD_REQUEST);
        }
        String mobile = request.getMobileNumber().trim();

        if ("warden".equalsIgnoreCase(username) || "admin".equalsIgnoreCase(username)) {
            throw new ApiException("Username '" + username + "' is reserved for system administration", HttpStatus.BAD_REQUEST);
        }

        if (request.getRole() != null && ("WARDEN".equalsIgnoreCase(request.getRole()) || "ADMIN".equalsIgnoreCase(request.getRole()))) {
            throw new ApiException("Public registration cannot assign administrative roles", HttpStatus.BAD_REQUEST);
        }

        if (userRepository.existsByUsername(username)) {
            throw new ApiException("Username '" + username + "' is already taken", HttpStatus.CONFLICT);
        }
        if (userRepository.existsByEmail(email)) {
            throw new ApiException("Email address is already registered.", HttpStatus.CONFLICT);
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

        // Public registration ALWAYS assigns role USER.
        Role role = Role.USER;

        // Auto-generate unique Student ID for students
        String studentId = studentIdGeneratorService.generateNextStudentId();

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
                .studentId(studentId)
                .emailVerified(true)
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

    @Transactional
    public AuthResponse getCurrentUser(UserPrincipal principal) {
        if (principal == null || principal.getUser() == null) {
            throw new ApiException("User not authenticated", HttpStatus.UNAUTHORIZED);
        }
        User user = userRepository.findById(principal.getUser().getId())
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        // Lazily assign unique Student ID to existing students without one
        if (user.getRole() == Role.USER && (user.getStudentId() == null || user.getStudentId().isBlank())) {
            user.setStudentId(studentIdGeneratorService.generateNextStudentId());
            user = userRepository.save(user);
        }

        return mapToAuthResponse(user, null);
    }

    @Transactional
    public AuthResponse updateProfile(Long userId, ProfileUpdateRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        String rawEmail = req.getEmail();
        if (rawEmail == null || rawEmail.isBlank()) {
            throw new ApiException("Email is required", HttpStatus.BAD_REQUEST);
        }
        if (rawEmail.chars().anyMatch(Character::isUpperCase)) {
            throw new ApiException("Email must be in lowercase.", HttpStatus.BAD_REQUEST);
        }
        String newEmail = rawEmail.trim().toLowerCase();
        if (!newEmail.matches("^[a-z0-9._%+-]+@([a-z0-9-]+\\.)+[a-z]{2,}$")) {
            throw new ApiException("Please enter a valid email address (e.g., username@domain.com).", HttpStatus.BAD_REQUEST);
        }
        String newMobile = req.getMobileNumber().trim();

        boolean emailChanged = !user.getEmail().equalsIgnoreCase(newEmail);

        // Check if email changed: requires uniqueness and updates email directly without OTP
        if (emailChanged) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new ApiException("Email address '" + newEmail + "' is already in use by another account", HttpStatus.CONFLICT);
            }
            user.setEmail(newEmail);
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


    @Transactional
    public AuthResponse updateProfilePhoto(Long userId, String photoUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        user.setProfilePhoto(photoUrl);
        user.setUpdatedAt(LocalDateTime.now());
        User saved = userRepository.save(user);
        return mapToAuthResponse(saved, null);
    }

    // ==========================================
    // FORGOT PASSWORD FLOWS (EMAIL & MOBILE)
    // ==========================================

    @Transactional
    public void forgotPasswordRequestOtp(String rawIdentifier, String rawType) {
        if (rawIdentifier == null || rawIdentifier.isBlank()) {
            throw new ApiException("Please enter your registered email or mobile number", HttpStatus.BAD_REQUEST);
        }

        OtpVerification.OtpType type;
        if ("MOBILE".equalsIgnoreCase(rawType)) {
            type = OtpVerification.OtpType.PASSWORD_RESET_MOBILE;
            String mobile = normalizeMobile(rawIdentifier);
            if (!userRepository.existsByMobileNumber(mobile)) {
                throw new ApiException("No registered account found with mobile number " + mobile, HttpStatus.NOT_FOUND);
            }
            otpService.sendPasswordResetOtp(mobile, type);
        } else {
            type = OtpVerification.OtpType.PASSWORD_RESET_EMAIL;
            String email = rawIdentifier.trim().toLowerCase();
            if (!userRepository.existsByEmail(email)) {
                throw new ApiException("No registered account found with email address " + email, HttpStatus.NOT_FOUND);
            }
            otpService.sendPasswordResetOtp(email, type);
        }
    }

    @Transactional
    public String forgotPasswordVerifyOtp(String rawIdentifier, String rawType, String otp) {
        boolean isMobile = "MOBILE".equalsIgnoreCase(rawType);
        String cleanIdentifier = isMobile
                ? normalizeMobile(rawIdentifier)
                : rawIdentifier.trim().toLowerCase();

        OtpVerification.OtpType type = isMobile
                ? OtpVerification.OtpType.PASSWORD_RESET_MOBILE
                : OtpVerification.OtpType.PASSWORD_RESET_EMAIL;

        return otpService.verifyPasswordResetOtp(cleanIdentifier, type, otp);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new ApiException("Passwords do not match", HttpStatus.BAD_REQUEST);
        }

        boolean isMobile = "MOBILE".equalsIgnoreCase(req.getType());
        OtpVerification.OtpType type = isMobile
                ? OtpVerification.OtpType.PASSWORD_RESET_MOBILE
                : OtpVerification.OtpType.PASSWORD_RESET_EMAIL;

        String cleanIdentifier = isMobile
                ? normalizeMobile(req.getIdentifier())
                : req.getIdentifier().trim().toLowerCase();

        otpService.validateAndConsumeResetToken(cleanIdentifier, type, req.getResetToken());

        User user;
        if (isMobile) {
            user = userRepository.findByMobileNumber(cleanIdentifier)
                    .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        } else {
            user = userRepository.findByEmail(cleanIdentifier)
                    .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    private static final java.util.regex.Pattern INDIAN_MOBILE_REGEX = java.util.regex.Pattern.compile("^[6-9]\\d{9}$");

    private String normalizeMobile(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            throw new ApiException("Please enter your registered mobile number", HttpStatus.BAD_REQUEST);
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.length() == 12 && digits.startsWith("91")) {
            digits = digits.substring(2);
        } else if (digits.length() == 11 && digits.startsWith("0")) {
            digits = digits.substring(1);
        }
        if (!INDIAN_MOBILE_REGEX.matcher(digits).matches()) {
            throw new ApiException("Please enter a valid 10-digit Indian mobile number starting with 6-9.", HttpStatus.BAD_REQUEST);
        }
        return digits;
    }

    public OtpService getOtpService() {
        return this.otpService;
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
                .studentId(user.getStudentId())
                .profilePhoto(user.getProfilePhoto())
                .emailVerified(user.isEmailVerified())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
