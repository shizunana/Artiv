package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.dto.ConversationSummary;
import com.huiyue.dto.PrivateMessageRequest;
import com.huiyue.entity.Message;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;

public interface MessageService {

    Message sendPrivateMessage(Long fromUserId, PrivateMessageRequest request);

    Page<Message> listConversation(Long userId, Long withUserId, Long projectId, Long orderId, long page, long size);

    Page<ConversationSummary> listConversations(Long userId, long page, long size);

    long countUnread(Long userId);

    Page<Message> listNotifications(Long userId, long page, long size);

    Message createNotification(Long toUserId, Integer type, String subtype, Long projectId, String content);

    void createProjectConversation(Long employerId, Long painterId, Long projectId);

    void createStallPurchaseCommunication(OrderInfo order, Stall stall);
}
