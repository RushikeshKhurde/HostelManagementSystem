package com.hostel.management.repository;

import com.hostel.management.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<LeaveRequest> findAllByOrderByCreatedAtDesc();
    long countByStatus(LeaveRequest.LeaveStatus status);
}
