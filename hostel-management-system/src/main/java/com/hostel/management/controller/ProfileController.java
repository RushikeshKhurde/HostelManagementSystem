package com.hostel.management.controller;

import com.hostel.management.dto.AuthResponse;
import com.hostel.management.dto.ProfileUpdateRequest;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.AuthService;
import com.hostel.management.service.FileStorageService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final AuthService authService;
    private final FileStorageService fileStorageService;

    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(authService.getCurrentUser(principal));
    }

    @PutMapping
    public ResponseEntity<AuthResponse> updateProfile(@AuthenticationPrincipal UserPrincipal principal,
                                                      @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(authService.updateProfile(principal.getUser().getId(), request));
    }


    @PostMapping(value = "/photo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AuthResponse> uploadProfilePhoto(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("file") MultipartFile file) {
        String photoUrl = fileStorageService.storeProfilePhoto(file);
        return ResponseEntity.ok(authService.updateProfilePhoto(principal.getUser().getId(), photoUrl));
    }
}
