package com.artiv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FollowedPainterItem {
    private Long painterId;
    private String nickname;
    private String avatar;
    private String bio;
    private String tags;
    private LocalDateTime followedAt;
}
