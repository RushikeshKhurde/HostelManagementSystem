package com.hostel.management.controller;

import com.hostel.management.dto.LeaveRequestDto;
import com.hostel.management.dto.LeaveResponse;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.LeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaves")
@RequiredArgsConstructor
public class LeaveController {

    private final LeaveService leaveService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LeaveResponse> applyLeave(@AuthenticationPrincipal UserPrincipal principal,
                                                    @Valid @RequestBody LeaveRequestDto request) {
        return ResponseEntity.ok(leaveService.applyLeave(principal.getUser(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<LeaveResponse>> getMyLeaves(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leaveService.getLeavesForStudent(principal.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<List<LeaveResponse>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'WARDEN')")
    public ResponseEntity<LeaveResponse> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String status = body != null ? body.get("status") : null;
        String remarks = body != null ? body.get("adminRemarks") : null;
        return ResponseEntity.ok(leaveService.updateStatus(id, status, remarks));
    }
}
