package com.hostel.management.service;

import com.hostel.management.exception.ApiException;
import com.hostel.management.model.Complaint;
import com.hostel.management.model.User;
import com.hostel.management.repository.ComplaintRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComplaintService {

    private final ComplaintRepository complaintRepository;

    public Complaint createComplaint(User student, Complaint complaint) {
        complaint.setStudent(student);
        complaint.setStatus(Complaint.ComplaintStatus.PENDING);
        return complaintRepository.save(complaint);
    }

    public List<Complaint> getComplaintsForStudent(Long studentId) {
        return complaintRepository.findByStudentIdOrderByCreatedAtDesc(studentId);
    }

    public List<Complaint> getAllComplaints() {
        return complaintRepository.findAllByOrderByCreatedAtDesc();
    }

    public Complaint updateStatus(Long complaintId, String statusStr, String adminComment) {
        Complaint complaint = complaintRepository.findById(complaintId)
                .orElseThrow(() -> new ApiException("Complaint not found", HttpStatus.NOT_FOUND));

        try {
            complaint.setStatus(Complaint.ComplaintStatus.valueOf(statusStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            throw new ApiException("Invalid complaint status", HttpStatus.BAD_REQUEST);
        }

        if (adminComment != null) {
            complaint.setAdminComment(adminComment);
        }

        return complaintRepository.save(complaint);
    }
}
