package com.artiv.dto;

/** 消息中心按展示分区汇总的未读数量。 */
public record MessageUnreadSummary(
        long totalUnreadCount,
        long conversationUnreadCount,
        long notificationUnreadCount
) {
}
