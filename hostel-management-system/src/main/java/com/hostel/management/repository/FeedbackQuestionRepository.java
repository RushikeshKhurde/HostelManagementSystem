package com.hostel.management.repository;

import com.hostel.management.model.FeedbackQuestion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackQuestionRepository extends JpaRepository<FeedbackQuestion, Long> {

    @Query("SELECT DISTINCT q FROM FeedbackQuestion q LEFT JOIN FETCH q.options o WHERE q.active = true ORDER BY q.displayOrder ASC")
    List<FeedbackQuestion> findActiveQuestionsWithOptions();

    List<FeedbackQuestion> findByActiveTrueOrderByDisplayOrderAsc();

    Optional<FeedbackQuestion> findByIdAndActiveTrue(Long id);

    long countByActiveTrue();
}
