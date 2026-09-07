package com.hostel.management.controller;

import com.hostel.management.model.LeaveRequest;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.LeaveService;
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
    public ResponseEntity<LeaveRequest> applyLeave(@AuthenticationPrincipal UserPrincipal principal,
                                                   @RequestBody LeaveRequest request) {
        return ResponseEntity.ok(leaveService.applyLeave(principal.getUser(), request));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<LeaveRequest>> getMyLeaves(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(leaveService.getLeavesForStudent(principal.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<LeaveRequest>> getAllLeaves() {
        return ResponseEntity.ok(leaveService.getAllLeaves());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LeaveRequest> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(leaveService.updateStatus(id, body.get("status"), body.get("adminRemarks")));
    }
}
