package com.artiv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderReviewContext {
    private String status;
    private LocalDateTime deadline;
    private ReviewListItem review;
}
