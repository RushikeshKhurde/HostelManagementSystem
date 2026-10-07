package com.hostel.management.repository;

import com.hostel.management.model.FeedbackQuestionOption;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackQuestionOptionRepository extends JpaRepository<FeedbackQuestionOption, Long> {

    List<FeedbackQuestionOption> findByQuestionIdOrderByDisplayOrderAsc(Long questionId);

    Optional<FeedbackQuestionOption> findByIdAndQuestionId(Long id, Long questionId);
}
