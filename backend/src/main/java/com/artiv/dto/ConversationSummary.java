package com.artiv.dto;

import lombok.Data;

import java.time.LocalDateTime;

/** 消息中心左侧的一条会话摘要。 */
@Data
public class ConversationSummary {

    private Long withUserId;

    private String withUserNickname;

    private String withUserAvatar;

    private Long projectId;

    private Long orderId;

    private String conversationType;

    private String lastContent;

    private LocalDateTime lastCreatedAt;

    private long unreadCount;
}
