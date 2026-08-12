package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.BusinessException;
import com.huiyue.common.ResultCode;
import com.huiyue.dto.ConversationSummary;
import com.huiyue.dto.PrivateMessageRequest;
import com.huiyue.entity.Application;
import com.huiyue.entity.Message;
import com.huiyue.entity.Project;
import com.huiyue.entity.User;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;
import com.huiyue.mapper.ApplicationMapper;
import com.huiyue.mapper.MessageMapper;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.mapper.OrderInfoMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.MessageService;
import com.huiyue.websocket.MessageWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    public static final int TYPE_SYSTEM = 0;
    public static final int TYPE_APPLICATION = 1;
    public static final int TYPE_ORDER = 2;
    public static final int TYPE_PRIVATE = 3;
    public static final String SUBTYPE_PRIVATE_CHAT = "PRIVATE_CHAT";

    private final MessageMapper messageMapper;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final ApplicationMapper applicationMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final MessageWebSocketHandler messageWebSocketHandler;
    private final ObjectMapper objectMapper;

    @Override
    public Message sendPrivateMessage(Long fromUserId, PrivateMessageRequest request) {
        if (fromUserId == null || request == null || request.getToId() == null) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty() || content.length() > 500 || fromUserId.equals(request.getToId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        User recipient = userMapper.selectById(request.getToId());
        if (recipient == null || !Integer.valueOf(0).equals(recipient.getDeleted()) || !Integer.valueOf(0).equals(recipient.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        validateProjectConversation(fromUserId, request.getToId(), request.getProjectId());
        validateOrderConversation(fromUserId, request.getToId(), request.getOrderId());
        if (request.getProjectId() != null && request.getOrderId() != null) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }

        Message message = new Message();
        message.setFromId(fromUserId);
        message.setToId(request.getToId());
        message.setProjectId(request.getProjectId());
        message.setOrderId(request.getOrderId());
        message.setType(TYPE_PRIVATE);
        message.setSubtype(SUBTYPE_PRIVATE_CHAT);
        message.setConversationKey(conversationKey(fromUserId, request.getToId()));
        message.setContent(content);
        message.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        messageMapper.insert(message);
        push(message, "NEW_MESSAGE");
        return message;
    }

    @Override
    public Page<Message> listConversation(Long userId, Long withUserId, Long projectId, Long orderId, long page, long size) {
        if (userId == null || withUserId == null || userId.equals(withUserId)) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        validateProjectConversation(userId, withUserId, projectId);
        validateOrderConversation(userId, withUserId, orderId);
        if (projectId != null && orderId != null) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getType, TYPE_PRIVATE)
                .eq(Message::getConversationKey, conversationKey(userId, withUserId))
                .eq(projectId != null, Message::getProjectId, projectId)
                .isNull(projectId == null, Message::getProjectId)
                .eq(orderId != null, Message::getOrderId, orderId)
                .isNull(orderId == null, Message::getOrderId)
                .orderByAsc(Message::getCreatedAt);
        return messageMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Page<ConversationSummary> listConversations(Long userId, long page, long size) {
        List<Message> messages = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getType, TYPE_PRIVATE)
                .and(w -> w.eq(Message::getFromId, userId).or().eq(Message::getToId, userId))
                .orderByDesc(Message::getCreatedAt));
        Map<String, ConversationSummary> grouped = new LinkedHashMap<>();
        for (Message message : messages) {
            String key = message.getConversationKey() + ":project:"
                    + (message.getProjectId() == null ? "none" : message.getProjectId())
                    + ":order:" + (message.getOrderId() == null ? "none" : message.getOrderId());
            ConversationSummary summary = grouped.get(key);
            if (summary == null) {
                summary = new ConversationSummary();
                Long withUserId = userId.equals(message.getFromId()) ? message.getToId() : message.getFromId();
                summary.setWithUserId(withUserId);
                summary.setProjectId(message.getProjectId());
                summary.setOrderId(message.getOrderId());
                summary.setConversationType(message.getOrderId() == null ? "PRIVATE" : "TRADE");
                summary.setLastContent(message.getContent());
                summary.setLastCreatedAt(message.getCreatedAt());
                User other = userMapper.selectById(withUserId);
                if (other != null) {
                    summary.setWithUserNickname(other.getNickname());
                    summary.setWithUserAvatar(other.getAvatar());
                }
                grouped.put(key, summary);
            }
            if (userId.equals(message.getToId()) && message.getReadAt() == null) {
                summary.setUnreadCount(summary.getUnreadCount() + 1);
            }
        }
        List<ConversationSummary> summaries = new ArrayList<>(grouped.values());
        summaries.sort(Comparator.comparing(ConversationSummary::getLastCreatedAt).reversed());
        int fromIndex = (int) Math.min(Math.max(0, (page - 1) * size), summaries.size());
        int toIndex = (int) Math.min(fromIndex + size, summaries.size());
        Page<ConversationSummary> result = new Page<>(page, size, summaries.size());
        result.setRecords(summaries.subList(fromIndex, toIndex));
        return result;
    }

    @Override
    public long countUnread(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getToId, userId)
                .isNull(Message::getReadAt));
    }

    @Override
    public Page<Message> listNotifications(Long userId, long page, long size) {
        return messageMapper.selectPage(new Page<>(page, size), new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getToId, userId)
                .ne(Message::getType, TYPE_PRIVATE)
                .orderByDesc(Message::getCreatedAt));
    }

    @Override
    public Message createNotification(Long toUserId, Integer type, String subtype, Long projectId, String content) {
        Message message = new Message();
        message.setToId(toUserId);
        message.setProjectId(projectId);
        message.setType(type);
        message.setSubtype(subtype);
        message.setContent(content);
        message.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        messageMapper.insert(message);
        push(message, type == TYPE_APPLICATION ? "APPLICATION" : "SYSTEM");
        return message;
    }

    @Override
    public void createProjectConversation(Long employerId, Long painterId, Long projectId) {
        Message message = new Message();
        message.setFromId(employerId);
        message.setToId(painterId);
        message.setProjectId(projectId);
        message.setType(TYPE_PRIVATE);
        message.setSubtype("APPLICATION_CONVERSATION_STARTED");
        message.setConversationKey(conversationKey(employerId, painterId));
        message.setContent("企划方已选择你的应征，双方可在这里继续沟通需求。");
        saveAndPush(message, "NEW_MESSAGE");
    }

    @Override
    public void createStallPurchaseCommunication(OrderInfo order, Stall stall) {
        createNotification(order.getEmployerId(), TYPE_SYSTEM, "STALL_PURCHASED", null,
                "已购买橱窗「" + stall.getTitle() + "」，订单已创建，可进入我的消息与画师沟通。");
        createNotification(order.getPainterId(), TYPE_SYSTEM, "STALL_PURCHASE_RECEIVED", null,
                "企划方已购买你的橱窗「" + stall.getTitle() + "」，请进入我的消息与对方沟通。");
        Message message = new Message();
        message.setFromId(order.getEmployerId());
        message.setToId(order.getPainterId());
        message.setOrderId(order.getId());
        message.setType(TYPE_PRIVATE);
        message.setSubtype("STALL_PURCHASE_CONVERSATION_STARTED");
        message.setConversationKey(conversationKey(order.getEmployerId(), order.getPainterId()));
        message.setContent("已购买橱窗「" + stall.getTitle() + "」，双方可在这里沟通委托需求。");
        saveAndPush(message, "NEW_MESSAGE");
    }

    private void validateProjectConversation(Long fromUserId, Long toUserId, Long projectId) {
        if (projectId == null) {
            return;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        boolean employerAndApplicant = fromUserId.equals(project.getEmployerId()) && hasApplication(projectId, toUserId);
        boolean applicantAndEmployer = toUserId.equals(project.getEmployerId()) && hasApplication(projectId, fromUserId);
        if (!employerAndApplicant && !applicantAndEmployer) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }

    private boolean hasApplication(Long projectId, Long painterId) {
        return applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getDeleted, 0)
                .eq(Application::getProjectId, projectId)
                .eq(Application::getPainterId, painterId)) > 0;
    }

    private void validateOrderConversation(Long fromUserId, Long toUserId, Long orderId) {
        if (orderId == null) {
            return;
        }
        OrderInfo order = orderInfoMapper.selectById(orderId);
        if (order == null || !Integer.valueOf(0).equals(order.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        boolean matched = (fromUserId.equals(order.getEmployerId()) && toUserId.equals(order.getPainterId()))
                || (fromUserId.equals(order.getPainterId()) && toUserId.equals(order.getEmployerId()));
        if (!matched) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }

    private String conversationKey(Long firstUserId, Long secondUserId) {
        long min = Math.min(firstUserId, secondUserId);
        long max = Math.max(firstUserId, secondUserId);
        return min + "_" + max;
    }

    private void push(Message message, String eventType) {
        try {
            Map<String, Object> event = Map.of("type", eventType, "data", message);
            messageWebSocketHandler.pushToUser(message.getToId(), objectMapper.writeValueAsString(event));
        } catch (Exception ignored) {
            // 已落库的消息不因实时推送失败而回滚，客户端重连后会补拉。
        }
    }

    private void saveAndPush(Message message, String eventType) {
        message.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        message.setCreatedAt(now);
        message.setUpdatedAt(now);
        messageMapper.insert(message);
        push(message, eventType);
    }
}
