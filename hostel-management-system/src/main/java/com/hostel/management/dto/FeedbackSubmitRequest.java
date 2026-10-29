package com.hostel.management.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedbackSubmitRequest {

    @NotEmpty(message = "Answers list cannot be empty")
    @Valid
    private List<FeedbackAnswerRequest> answers;

    @Size(max = 5000, message = "Description cannot exceed 5000 characters")
    private String description;
}
