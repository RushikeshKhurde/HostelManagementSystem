package com.hostel.management.repository;

import com.hostel.management.model.Complaint;
import com.hostel.management.model.ComplaintVisibility;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {
    List<Complaint> findByStudentIdOrderByCreatedAtDesc(Long studentId);
    List<Complaint> findAllByOrderByCreatedAtDesc();
    List<Complaint> findByVisibilityInOrderByCreatedAtDesc(Collection<ComplaintVisibility> visibilities);
    long countByStatus(Complaint.ComplaintStatus status);
    long countByStatusAndVisibilityIn(Complaint.ComplaintStatus status, Collection<ComplaintVisibility> visibilities);
}
