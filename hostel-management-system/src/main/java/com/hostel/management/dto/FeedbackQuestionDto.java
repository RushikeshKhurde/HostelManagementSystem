package com.hostel.management.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackQuestionDto {
    private Long id;
    private String questionText;
    private String category;
    private Integer displayOrder;
    private List<FeedbackOptionDto> options;
}
