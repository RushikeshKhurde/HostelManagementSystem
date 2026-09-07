package com.hostel.management.controller;

import com.hostel.management.model.Complaint;
import com.hostel.management.security.UserPrincipal;
import com.hostel.management.service.ComplaintService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/complaints")
@RequiredArgsConstructor
public class ComplaintController {

    private final ComplaintService complaintService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Complaint> createComplaint(@AuthenticationPrincipal UserPrincipal principal,
                                                      @RequestBody Complaint complaint) {
        return ResponseEntity.ok(complaintService.createComplaint(principal.getUser(), complaint));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<Complaint>> getMyComplaints(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(complaintService.getComplaintsForStudent(principal.getUser().getId()));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Complaint>> getAllComplaints() {
        return ResponseEntity.ok(complaintService.getAllComplaints());
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Complaint> updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return ResponseEntity.ok(complaintService.updateStatus(id, body.get("status"), body.get("adminComment")));
    }
}
