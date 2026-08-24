package com.artiv.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class FavoriteStallItem {
    private Long stallId;
    private Long painterId;
    private String title;
    private String category;
    private String cover;
    private BigDecimal price;
    private Integer stock;
    private String deliverTime;
    private String painterNickname;
    private String painterAvatar;
    private Long soldCount;
    private LocalDateTime favoritedAt;
}
