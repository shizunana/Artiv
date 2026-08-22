package com.artiv.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ReviewListItem {
    private Long reviewId;
    private Long orderId;
    private Long reviewerId;
    private String reviewerNickname;
    private String reviewerAvatar;
    private String reviewerRole;
    private Integer score;
    private String content;
    private List<String> images;
    private String sourceType;
    private String sourceTitle;
    private LocalDateTime createdAt;
    private boolean canAppeal;
    private Integer appealStatus;
}
