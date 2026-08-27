package com.artiv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoriteWorkItem {
    private Long workId;
    private Long userId;
    private String title;
    private String category;
    private String subject;
    private String image;
    private String description;
    private String authorNickname;
    private String authorAvatar;
    private LocalDateTime favoritedAt;
    private Long favoriteCount;
}
