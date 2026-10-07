package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackAnswerDto {
    private Long questionId;
    private String question;
    private String category;
    private Integer displayOrder;
    private Long selectedOptionId;
    private String selectedOption;
    private String selectedOptionValue;
}
