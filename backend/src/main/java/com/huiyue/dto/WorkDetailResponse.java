package com.huiyue.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 公开作品详情：作品基础字段与画师展示信息。 */
@Data
public class WorkDetailResponse {

    private Long id;
    private Long userId;
    private String title;
    private String category;
    private String subject;
    private String image;
    private String description;
    private LocalDateTime createdAt;

    private String authorName;
    private String authorAvatar;
    private String authorBio;
    private Integer authorCertStatus;
}
