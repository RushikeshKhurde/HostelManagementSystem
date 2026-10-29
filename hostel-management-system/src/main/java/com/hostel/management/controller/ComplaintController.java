package com.hostel.management.controller;

import com.hostel.management.dto.ComplaintCreateDto;
import com.hostel.management.dto.ComplaintRequest;
import com.hostel.management.dto.ComplaintResponse;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.ComplaintService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ComplaintResponse> createComplaintMultipart(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam("feedback") String feedback,
            @RequestParam(value = "visibility", required = false) String visibility,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "roomNumber", required = false) String roomNumber,
            @RequestParam(value = "priority", required = false) String priority,
            @RequestParam(value = "image", required = false) MultipartFile image) {

        ComplaintCreateDto dto = ComplaintCreateDto.builder()
                .feedback(feedback)
                .description(description)
                .title(title)
                .category(category)
                .roomNumber(roomNumber)
                .priority(priority)
                .visibility(visibility)
                .image(image)
                .build();

        return ResponseEntity.ok(complaintService.createComplaint(principal.getUser(), dto));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ComplaintResponse> createComplaintJson(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ComplaintRequest request) {
        return ResponseEntity.ok(complaintService.createComplaint(principal.getUser(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ComplaintResponse>> getMyComplaints(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(complaintService.getComplaintsForStudent(principal.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<List<ComplaintResponse>> getAllComplaints(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(complaintService.getComplaintsForStaff(principal.getUser()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'WARDEN')")
    public ResponseEntity<ComplaintResponse> getComplaintById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(complaintService.getComplaintById(id, principal.getUser()));
    }

    @GetMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'WARDEN')")
    public ResponseEntity<Resource> getComplaintImage(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {

        ComplaintService.ComplaintImageResource imageInfo = complaintService.getComplaintImage(id, principal.getUser());
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(imageInfo.getContentType());
        } catch (Exception e) {
            mediaType = MediaType.APPLICATION_OCTET_STREAM;
        }

        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + (imageInfo.getFilename() != null ? imageInfo.getFilename() : "complaint-image") + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "private, max-age=3600")
                .body(imageInfo.getResource());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<ComplaintResponse> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserPrincipal principal) {
        String status = body != null ? body.get("status") : null;
        String adminComment = body != null ? body.get("adminComment") : null;
        return ResponseEntity.ok(complaintService.updateStatus(id, status, adminComment, principal != null ? principal.getUser() : null));
    }
}
