package com.artiv.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BlockedUserItem {
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer role;
    private Integer certStatus;
    private Integer deleted;
    private LocalDateTime blockedAt;
}
