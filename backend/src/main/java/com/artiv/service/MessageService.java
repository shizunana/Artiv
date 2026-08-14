package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.ConversationSummary;
import com.artiv.dto.PrivateMessageRequest;
import com.artiv.entity.Message;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;

public interface MessageService {

    Message sendPrivateMessage(Long fromUserId, PrivateMessageRequest request);

    Page<Message> listConversation(Long userId, Long withUserId, Long projectId, Long orderId, long page, long size);

    Page<ConversationSummary> listConversations(Long userId, long page, long size);

    long countUnread(Long userId);

    Page<Message> listNotifications(Long userId, long page, long size);

    Message createNotification(Long toUserId, Integer type, String subtype, Long projectId, String content);

    Message startProjectConversation(Long employerId, Long projectId, Long painterId);

    void createStallPurchaseCommunication(OrderInfo order, Stall stall);

    void createProjectOrderCommunication(OrderInfo order, Project project);

    void createOrderEventMessage(OrderInfo order, Long actorId, String subtype, String content);
}
