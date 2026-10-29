package com.hostel.management.repository;

import com.hostel.management.model.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {

    List<LeaveRequest> findByStudentIdOrderByCreatedAtDesc(Long studentId);

    List<LeaveRequest> findAllByOrderByCreatedAtDesc();

    long countByStatus(LeaveRequest.LeaveStatus status);

    @Query("SELECT l FROM LeaveRequest l " +
           "WHERE l.student.id = :studentId " +
           "AND l.status = :status " +
           "AND l.startDate <= :newEnd " +
           "AND l.endDate >= :newStart " +
           "ORDER BY l.startDate ASC")
    List<LeaveRequest> findOverlappingLeaves(
            @Param("studentId") Long studentId,
            @Param("status") LeaveRequest.LeaveStatus status,
            @Param("newStart") LocalDate newStart,
            @Param("newEnd") LocalDate newEnd
    );

    @Query("SELECT l FROM LeaveRequest l " +
           "WHERE l.student.id = :studentId " +
           "AND l.status = :status " +
           "AND l.id <> :excludeLeaveId " +
           "AND l.startDate <= :newEnd " +
           "AND l.endDate >= :newStart " +
           "ORDER BY l.startDate ASC")
    List<LeaveRequest> findOverlappingLeavesExcluding(
            @Param("studentId") Long studentId,
            @Param("status") LeaveRequest.LeaveStatus status,
            @Param("newStart") LocalDate newStart,
            @Param("newEnd") LocalDate newEnd,
            @Param("excludeLeaveId") Long excludeLeaveId
    );

    @Query("SELECT COUNT(l) > 0 FROM LeaveRequest l " +
           "WHERE l.student.id = :studentId " +
           "AND l.status = :status " +
           "AND l.startDate <= :newEnd " +
           "AND l.endDate >= :newStart")
    boolean existsOverlappingLeave(
            @Param("studentId") Long studentId,
            @Param("status") LeaveRequest.LeaveStatus status,
            @Param("newStart") LocalDate newStart,
            @Param("newEnd") LocalDate newEnd
    );
}
