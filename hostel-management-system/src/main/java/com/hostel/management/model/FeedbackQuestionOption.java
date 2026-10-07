package com.hostel.management.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "feedback_question_options")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackQuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    @JsonIgnore
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private FeedbackQuestion question;

    @Column(name = "option_value", nullable = false, length = 50)
    private String optionValue;

    @Column(name = "display_text", nullable = false, length = 100)
    private String displayText;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;
}
