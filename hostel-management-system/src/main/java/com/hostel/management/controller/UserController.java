package com.hostel.management.controller;

import com.hostel.management.dto.UserResponse;
import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Role;
import com.hostel.management.model.User;
import com.hostel.management.repository.UserRepository;
import com.hostel.management.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<List<UserResponse>> getAllUsers() {
        List<UserResponse> users = userRepository.findAll().stream()
                .map(UserResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(users);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> updateUserStatus(@PathVariable Long id,
                                                         @AuthenticationPrincipal UserPrincipal principal,
                                                         @RequestBody Map<String, String> body) {
        if (body == null || !body.containsKey("status") || body.get("status") == null || body.get("status").isBlank()) {
            throw new ApiException("Status is required", HttpStatus.BAD_REQUEST);
        }

        String status = body.get("status").trim().toUpperCase();
        if (!"ACTIVE".equals(status) && !"INACTIVE".equals(status)) {
            throw new ApiException("Invalid status. Only ACTIVE or INACTIVE are allowed.", HttpStatus.BAD_REQUEST);
        }

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ApiException("User not found", HttpStatus.NOT_FOUND));

        if (targetUser.getRole() == Role.ADMIN) {
            throw new ApiException("Cannot change the status of an ADMIN account", HttpStatus.BAD_REQUEST);
        }

        if (principal != null && principal.getUser() != null && principal.getUser().getId().equals(targetUser.getId())) {
            throw new ApiException("Administrators cannot deactivate their own account", HttpStatus.BAD_REQUEST);
        }

        targetUser.setStatus(status);
        targetUser.setEnabled("ACTIVE".equalsIgnoreCase(status));

        User saved = userRepository.save(targetUser);
        return ResponseEntity.ok(UserResponse.fromEntity(saved));
    }
}
