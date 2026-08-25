package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.ConversationSummary;
import com.artiv.dto.MessageUnreadSummary;
import com.artiv.dto.PrivateMessageRequest;
import com.artiv.entity.Application;
import com.artiv.entity.Message;
import com.artiv.entity.Project;
import com.artiv.entity.User;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Stall;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.MessageMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.MessageService;
import com.artiv.websocket.MessageWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.artiv.common.SystemMessageText.markTitle;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    public static final int TYPE_SYSTEM = 0;
    public static final int TYPE_APPLICATION = 1;
    public static final int TYPE_ORDER = 2;
    public static final int TYPE_PRIVATE = 3;
    public static final String SUBTYPE_PRIVATE_CHAT = "PRIVATE_CHAT";
    public static final String SUBTYPE_PRIVATE_IMAGE = "PRIVATE_IMAGE";
    public static final String SUBTYPE_PRIVATE_ORDER_CARD = "PRIVATE_ORDER_CARD";
    private static final int MAX_PRIVATE_IMAGE_COUNT = 9;
    private static final String UPLOAD_PATH_PREFIX = "/uploads/";

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
        String textContent = request.getContent() == null ? "" : request.getContent().trim();
        List<String> images = normalizeImages(request.getImages());
        boolean hasText = !textContent.isEmpty();
        boolean hasImages = !images.isEmpty();
        if (hasText == hasImages || textContent.length() > 500 || fromUserId.equals(request.getToId())) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        User recipient = userMapper.selectById(request.getToId());
        if (recipient == null || !Integer.valueOf(0).equals(recipient.getDeleted()) || !Integer.valueOf(0).equals(recipient.getStatus())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (request.getProjectId() != null && request.getOrderId() != null) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        // 保留旧入口携带企划/订单上下文时的权限校验；消息中心合并会话后发送普通私信。
        validateProjectConversation(fromUserId, request.getToId(), request.getProjectId());
        validateOrderConversation(fromUserId, request.getToId(), request.getOrderId());

        Message message = new Message();
        message.setFromId(fromUserId);
        message.setToId(request.getToId());
        // 一对用户只维护一个沟通会话；企划或橱窗来源由系统消息本身保留关联 ID。
        message.setProjectId(null);
        boolean orderCard = request.getOrderId() != null;
        message.setOrderId(orderCard ? request.getOrderId() : null);
        message.setType(TYPE_PRIVATE);
        message.setSubtype(orderCard ? SUBTYPE_PRIVATE_ORDER_CARD : (hasImages ? SUBTYPE_PRIVATE_IMAGE : SUBTYPE_PRIVATE_CHAT));
        message.setConversationKey(conversationKey(fromUserId, request.getToId()));
        message.setContent(hasImages ? toImagePayload(images) : textContent);
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
        if (projectId != null && orderId != null) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getType, TYPE_PRIVATE)
                .eq(Message::getConversationKey, conversationKey(userId, withUserId))
                .orderByDesc(Message::getCreatedAt)
                .orderByDesc(Message::getId);
        Page<Message> result = messageMapper.selectPage(new Page<>(page, size), wrapper);
        result.getRecords().sort(Comparator
                .comparing(Message::getCreatedAt)
                .thenComparing(Message::getId));
        return result;
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
            String key = normalizedConversationKey(message);
            Long withUserId = conversationPartner(userId, message, key);
            if (key == null || withUserId == null) {
                continue;
            }
            ConversationSummary summary = grouped.get(key);
            if (summary == null) {
                summary = new ConversationSummary();
                summary.setWithUserId(withUserId);
                summary.setProjectId(message.getProjectId());
                summary.setOrderId(message.getOrderId());
                summary.setConversationType("DIRECT");
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
    public MessageUnreadSummary countUnreadSummary(Long userId) {
        long conversationUnreadCount = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getToId, userId)
                .eq(Message::getType, TYPE_PRIVATE)
                .isNull(Message::getReadAt));
        long notificationUnreadCount = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getToId, userId)
                .ne(Message::getType, TYPE_PRIVATE)
                .isNull(Message::getReadAt));
        return new MessageUnreadSummary(
                conversationUnreadCount + notificationUnreadCount,
                conversationUnreadCount,
                notificationUnreadCount
        );
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

    private void createProjectConversation(Long employerId, Long painterId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        String projectTitle = project == null ? "企划 #" + projectId : project.getTitle();
        Message message = new Message();
        message.setFromId(employerId);
        message.setToId(painterId);
        message.setProjectId(projectId);
        message.setType(TYPE_PRIVATE);
        message.setSubtype("APPLICATION_CONVERSATION_STARTED");
        message.setConversationKey(conversationKey(employerId, painterId));
        message.setContent("您对企划" + markTitle(projectTitle) + "的应征已被企划方选中，双方可在这里继续沟通需求。");
        saveAndPush(message, "NEW_MESSAGE");
    }

    @Override
    public Message startProjectConversation(Long employerId, Long projectId, Long painterId) {
        Project project = requireOwnedProject(employerId, projectId);
        requireActiveApplication(project.getId(), painterId);
        Message existing = findProjectConversationStarter(employerId, painterId, projectId);
        if (existing != null) {
            return existing;
        }
        Message message = new Message();
        message.setFromId(employerId);
        message.setToId(painterId);
        message.setProjectId(projectId);
        message.setType(TYPE_PRIVATE);
        message.setSubtype("APPLICATION_CONVERSATION_STARTED");
        message.setConversationKey(conversationKey(employerId, painterId));
        message.setStarterKey(projectConversationStarterKey(projectId, employerId, painterId));
        message.setContent("企划方已就企划" + markTitle(project.getTitle()) + "发起沟通，双方可在这里继续沟通需求。");
        try {
            saveAndPush(message, "NEW_MESSAGE");
            return message;
        } catch (DuplicateKeyException duplicateKeyException) {
            Message starter = findProjectConversationStarter(employerId, painterId, projectId);
            if (starter != null) {
                return starter;
            }
            throw duplicateKeyException;
        }
    }

    @Override
    public Message createInvitationAcceptedConversation(Long employerId, Long painterId,
                                                         Long projectId, String content) {
        Project project = requireOwnedProject(employerId, projectId);
        requireActiveApplication(project.getId(), painterId);
        Message existing = findProjectConversationStarter(employerId, painterId, projectId);
        if (existing != null) {
            Message message = new Message();
            message.setFromId(painterId);
            message.setToId(employerId);
            message.setProjectId(projectId);
            message.setType(TYPE_PRIVATE);
            message.setSubtype("INVITATION_ACCEPTED");
            message.setConversationKey(conversationKey(employerId, painterId));
            message.setContent(content);
            saveAndPush(message, "NEW_MESSAGE");
            return message;
        }
        Message message = new Message();
        message.setFromId(painterId);
        message.setToId(employerId);
        message.setProjectId(projectId);
        message.setType(TYPE_PRIVATE);
        message.setSubtype("INVITATION_ACCEPTED");
        message.setConversationKey(conversationKey(employerId, painterId));
        message.setStarterKey(projectConversationStarterKey(projectId, employerId, painterId));
        message.setContent(content);
        try {
            saveAndPush(message, "NEW_MESSAGE");
            return message;
        } catch (DuplicateKeyException duplicateKeyException) {
            Message starter = findProjectConversationStarter(employerId, painterId, projectId);
            if (starter != null) {
                return starter;
            }
            throw duplicateKeyException;
        }
    }

    @Override
    public void createStallPurchaseCommunication(OrderInfo order, Stall stall) {
        createOrderNotification(order.getEmployerId(), order.getId(), "STALL_PURCHASED",
                "已购买橱窗" + markTitle(stall.getTitle()) + "，订单已创建，可进入我的消息与画师沟通。", true);
        createOrderNotification(order.getPainterId(), order.getId(), "STALL_PURCHASE_RECEIVED",
                "企划方已购买你的橱窗" + markTitle(stall.getTitle()) + "，请进入我的消息与对方沟通。");
        Message message = new Message();
        message.setFromId(order.getEmployerId());
        message.setToId(order.getPainterId());
        message.setOrderId(order.getId());
        message.setType(TYPE_PRIVATE);
        message.setSubtype("STALL_PURCHASE_CONVERSATION_STARTED");
        message.setConversationKey(conversationKey(order.getEmployerId(), order.getPainterId()));
        message.setContent("已购买橱窗" + markTitle(stall.getTitle()) + "，双方可在这里沟通委托需求。");
        saveAndPush(message, "NEW_MESSAGE");
    }

    @Override
    public void createProjectOrderCommunication(OrderInfo order, Project project) {
        createOrderNotification(order.getEmployerId(), order.getId(), "PROJECT_ORDER_CREATED",
                "已向画师发送企划订单" + markTitle(project.getTitle()) + "，金额已托管，等待画师接单。", true);
        createOrderNotification(order.getPainterId(), order.getId(), "PROJECT_ORDER_RECEIVED",
                "你收到企划" + markTitle(project.getTitle()) + "的新订单，请确认是否接单。");
        Message message = new Message();
        message.setFromId(order.getEmployerId());
        message.setToId(order.getPainterId());
        message.setProjectId(project.getId());
        message.setOrderId(order.getId());
        message.setType(TYPE_PRIVATE);
        message.setSubtype("PROJECT_ORDER_CONVERSATION_STARTED");
        message.setConversationKey(conversationKey(order.getEmployerId(), order.getPainterId()));
        message.setContent("企划方已发送企划订单" + markTitle(project.getTitle()) + "，双方可继续沟通并推进交付。");
        saveAndPush(message, "NEW_MESSAGE");
    }

    @Override
    public void createOrderEventMessage(OrderInfo order, Long actorId, String subtype, String content) {
        createOrderNotification(order.getEmployerId(), order.getId(), subtype, content,
                actorId != null && actorId.equals(order.getEmployerId()));
        createOrderNotification(order.getPainterId(), order.getId(), subtype, content,
                actorId != null && actorId.equals(order.getPainterId()));
        Message message = new Message();
        boolean actorIsEmployer = actorId != null && actorId.equals(order.getEmployerId());
        boolean actorIsPainter = actorId != null && actorId.equals(order.getPainterId());
        // 管理员触发的仲裁事件仍属于交易双方的合并会话，不能把管理员误识别为会话对象。
        message.setFromId(actorIsEmployer ? order.getEmployerId() : order.getPainterId());
        message.setToId(actorIsEmployer ? order.getPainterId() : order.getEmployerId());
        if (!actorIsEmployer && !actorIsPainter) {
            message.setFromId(order.getPainterId());
            message.setToId(order.getEmployerId());
        }
        message.setOrderId(order.getId());
        message.setType(TYPE_PRIVATE);
        message.setSubtype(subtype);
        message.setConversationKey(conversationKey(order.getEmployerId(), order.getPainterId()));
        message.setContent(content);
        saveAndPush(message, "NEW_MESSAGE");
    }

    private void createOrderNotification(Long toUserId, Long orderId, String subtype, String content) {
        createOrderNotification(toUserId, orderId, subtype, content, false);
    }

    private void createOrderNotification(Long toUserId, Long orderId, String subtype, String content, boolean read) {
        Message message = new Message();
        message.setToId(toUserId);
        message.setOrderId(orderId);
        message.setType(TYPE_ORDER);
        message.setSubtype(subtype);
        message.setContent(content);
        if (read) {
            message.setReadAt(LocalDateTime.now());
        }
        saveAndPush(message, "SYSTEM");
    }

    private void validateProjectConversation(Long fromUserId, Long toUserId, Long projectId) {
        if (projectId == null) {
            return;
        }
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        Long painterId = fromUserId.equals(project.getEmployerId()) ? toUserId : fromUserId;
        boolean employerAndApplicant = fromUserId.equals(project.getEmployerId()) && hasApplication(projectId, painterId);
        boolean applicantAndEmployer = toUserId.equals(project.getEmployerId()) && hasApplication(projectId, painterId);
        if ((!employerAndApplicant && !applicantAndEmployer)
                || !hasStartedProjectConversation(project.getEmployerId(), painterId, projectId)) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }

    private boolean hasApplication(Long projectId, Long painterId) {
        return applicationMapper.selectCount(new LambdaQueryWrapper<Application>()
                .eq(Application::getDeleted, 0)
                .eq(Application::getProjectId, projectId)
                .eq(Application::getPainterId, painterId)) > 0;
    }

    private Project requireOwnedProject(Long employerId, Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null || !Integer.valueOf(0).equals(project.getDeleted())) {
            throw new BusinessException(ResultCode.RESOURCE_NOT_FOUND);
        }
        if (!employerId.equals(project.getEmployerId())) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
        return project;
    }

    private void requireActiveApplication(Long projectId, Long painterId) {
        Application application = applicationMapper.selectOne(new LambdaQueryWrapper<Application>()
                .eq(Application::getDeleted, 0)
                .eq(Application::getProjectId, projectId)
                .eq(Application::getPainterId, painterId)
                .eq(Application::getActive, 1));
        if (application == null) {
            throw new BusinessException(ResultCode.NO_PERMISSION);
        }
    }

    private boolean hasStartedProjectConversation(Long employerId, Long painterId, Long projectId) {
        return findProjectConversationStarter(employerId, painterId, projectId) != null;
    }

    private Message findProjectConversationStarter(Long employerId, Long painterId, Long projectId) {
        return messageMapper.selectOne(new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getType, TYPE_PRIVATE)
                .eq(Message::getProjectId, projectId)
                .and(wrapper -> wrapper.eq(Message::getStarterKey, projectConversationStarterKey(projectId, employerId, painterId))
                        .or(legacy -> legacy.eq(Message::getConversationKey, conversationKey(employerId, painterId))
                                .eq(Message::getSubtype, "APPLICATION_CONVERSATION_STARTED")
                                .isNull(Message::getStarterKey)))
                .orderByAsc(Message::getCreatedAt)
                .last("LIMIT 1"));
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

    private String normalizedConversationKey(Message message) {
        if (message.getConversationKey() != null && !message.getConversationKey().isBlank()) {
            return message.getConversationKey();
        }
        if (message.getFromId() == null || message.getToId() == null) {
            return null;
        }
        return conversationKey(message.getFromId(), message.getToId());
    }

    private Long conversationPartner(Long userId, Message message, String key) {
        if (key != null) {
            String[] participants = key.split("_");
            if (participants.length == 2) {
                try {
                    Long first = Long.valueOf(participants[0]);
                    Long second = Long.valueOf(participants[1]);
                    if (userId.equals(first)) return second;
                    if (userId.equals(second)) return first;
                } catch (NumberFormatException ignored) {
                    // 兼容旧数据，无法解析时回退到消息收发双方。
                }
            }
        }
        if (userId.equals(message.getFromId())) return message.getToId();
        if (userId.equals(message.getToId())) return message.getFromId();
        return null;
    }

    private String projectConversationStarterKey(Long projectId, Long employerId, Long painterId) {
        return projectId + ":" + conversationKey(employerId, painterId) + ":APPLICATION_CONVERSATION_STARTED";
    }

    private List<String> normalizeImages(List<String> requestImages) {
        if (requestImages == null || requestImages.isEmpty()) {
            return List.of();
        }
        if (requestImages.size() > MAX_PRIVATE_IMAGE_COUNT) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        Set<String> uniqueImages = new LinkedHashSet<>();
        for (String image : requestImages) {
            String normalized = image == null ? "" : image.trim();
            if (normalized.isEmpty() || !normalized.startsWith(UPLOAD_PATH_PREFIX)) {
                throw new BusinessException(ResultCode.PARAM_INVALID);
            }
            uniqueImages.add(normalized);
        }
        if (uniqueImages.isEmpty() || uniqueImages.size() > MAX_PRIVATE_IMAGE_COUNT) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
        return List.copyOf(uniqueImages);
    }

    private String toImagePayload(List<String> images) {
        try {
            String payload = objectMapper.writeValueAsString(Map.of("images", images));
            if (payload.length() > 500) {
                throw new BusinessException(ResultCode.PARAM_INVALID);
            }
            return payload;
        } catch (BusinessException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new BusinessException(ResultCode.PARAM_INVALID);
        }
    }

    private void push(Message message, String eventType) {
        Runnable action = () -> pushImmediately(message, eventType);
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    action.run();
                }
            });
            return;
        }
        action.run();
    }

    private void pushImmediately(Message message, String eventType) {
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
