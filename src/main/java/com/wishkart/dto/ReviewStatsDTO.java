package com.wishkart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatsDTO {

    private double averageRating;
    private int totalReviews;
    private int fiveStarCount;
    private int fourStarCount;
    private int threeStarCount;
    private int twoStarCount;
    private int oneStarCount;

    public static ReviewStatsDTO empty() {
        return ReviewStatsDTO.builder()
            .averageRating(0.0)
            .totalReviews(0)
            .fiveStarCount(0)
            .fourStarCount(0)
            .threeStarCount(0)
            .twoStarCount(0)
            .oneStarCount(0)
            .build();
    }
}
