package com.hostel.management.repository;

import com.hostel.management.model.FeedbackSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackSubmissionRepository extends JpaRepository<FeedbackSubmission, Long> {

    boolean existsByStudentId(Long studentId);

    Optional<FeedbackSubmission> findByStudentId(Long studentId);

    @Query("SELECT DISTINCT s FROM FeedbackSubmission s " +
           "LEFT JOIN FETCH s.student st " +
           "LEFT JOIN FETCH s.answers a " +
           "LEFT JOIN FETCH a.question q " +
           "LEFT JOIN FETCH a.selectedOption o " +
           "WHERE st.id = :studentId")
    Optional<FeedbackSubmission> findByStudentIdWithDetails(@Param("studentId") Long studentId);

    @Query("SELECT DISTINCT s FROM FeedbackSubmission s " +
           "LEFT JOIN FETCH s.student st " +
           "LEFT JOIN FETCH s.answers a " +
           "LEFT JOIN FETCH a.question q " +
           "LEFT JOIN FETCH a.selectedOption o " +
           "WHERE s.id = :id")
    Optional<FeedbackSubmission> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM FeedbackSubmission s " +
           "LEFT JOIN FETCH s.student st " +
           "LEFT JOIN FETCH s.answers a " +
           "LEFT JOIN FETCH a.question q " +
           "LEFT JOIN FETCH a.selectedOption o " +
           "ORDER BY s.submittedAt DESC")
    List<FeedbackSubmission> findAllWithDetails();
}
