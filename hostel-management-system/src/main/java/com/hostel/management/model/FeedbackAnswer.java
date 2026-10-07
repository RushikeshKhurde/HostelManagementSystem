package com.hostel.management.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "feedback_answers",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_feedback_submission_question", columnNames = {"submission_id", "question_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "submission_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FeedbackSubmission submission;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private FeedbackQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private FeedbackQuestionOption selectedOption;
}
