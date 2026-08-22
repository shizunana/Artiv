package com.artiv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewStatistics {
    private Double averageScore;
    private long reviewCount;
}
