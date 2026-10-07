package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSubmissionDto {
    private Long id;
    private Long studentId;
    private String studentName;
    private String studentUsername;
    private String studentEmail;
    private LocalDateTime submittedAt;
    private List<FeedbackAnswerDto> answers;
}
