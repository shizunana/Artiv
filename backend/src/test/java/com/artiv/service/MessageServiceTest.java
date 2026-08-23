package com.artiv.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.common.BusinessException;
import com.artiv.dto.PrivateMessageRequest;
import com.artiv.dto.ConversationSummary;
import com.artiv.entity.Application;
import com.artiv.entity.Message;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
import com.artiv.entity.Stall;
import com.artiv.entity.User;
import com.artiv.mapper.ApplicationMapper;
import com.artiv.mapper.MessageMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock private MessageMapper messageMapper;
    @Mock private UserMapper userMapper;
    @Mock private ProjectMapper projectMapper;
    @Mock private ApplicationMapper applicationMapper;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks private MessageServiceImpl messageService;

    @Test
    void sendPrivateMessageUsesCurrentUserAndNormalizedConversationKey() {
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L));
        PrivateMessageRequest request = request(2L, null, "hello");

        messageService.sendPrivateMessage(15L, request);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals(15L, captor.getValue().getFromId());
        assertEquals(2L, captor.getValue().getToId());
        assertEquals("2_15", captor.getValue().getConversationKey());
        assertEquals(3, captor.getValue().getType());
    }

    @Test
    void sendPrivateMessageRejectsSendingToSelf() {
        assertThrows(BusinessException.class, () -> messageService.sendPrivateMessage(15L, request(15L, null, "self")));
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void sendPrivateMessageStoresMultipleImagesAsOneImageMessage() {
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L));
        PrivateMessageRequest request = request(2L, null, null);
        request.setImages(List.of("/uploads/chat-a.png", "/uploads/chat-b.png"));

        messageService.sendPrivateMessage(15L, request);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals("PRIVATE_IMAGE", captor.getValue().getSubtype());
        assertEquals("{\"images\":[\"/uploads/chat-a.png\",\"/uploads/chat-b.png\"]}", captor.getValue().getContent());
    }

    @Test
    void createStallPurchaseCommunicationCreatesTwoNotificationsAndOneTradeConversation() {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        Stall stall = new Stall();
        stall.setTitle("commission");

        messageService.createStallPurchaseCommunication(order, stall);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertEquals("STALL_PURCHASED", captor.getAllValues().get(0).getSubtype());
        assertEquals("STALL_PURCHASE_RECEIVED", captor.getAllValues().get(1).getSubtype());
        assertEquals("STALL_PURCHASE_CONVERSATION_STARTED", captor.getAllValues().get(2).getSubtype());
        assertEquals(77L, captor.getAllValues().get(2).getOrderId());
    }

    @Test
    void createProjectOrderCommunicationCreatesOrderCardsWithProjectLinkage() {
        OrderInfo order = new OrderInfo();
        order.setId(78L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        Project project = new Project();
        project.setId(8L);
        project.setTitle("企划测试");

        messageService.createProjectOrderCommunication(order, project);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertEquals("PROJECT_ORDER_CREATED", captor.getAllValues().get(0).getSubtype());
        assertEquals("PROJECT_ORDER_RECEIVED", captor.getAllValues().get(1).getSubtype());
        assertEquals("PROJECT_ORDER_CONVERSATION_STARTED", captor.getAllValues().get(2).getSubtype());
        assertEquals(8L, captor.getAllValues().get(2).getProjectId());
        assertEquals(78L, captor.getAllValues().get(2).getOrderId());
        org.junit.jupiter.api.Assertions.assertNotNull(captor.getAllValues().get(0).getReadAt());
        org.junit.jupiter.api.Assertions.assertNull(captor.getAllValues().get(1).getReadAt());
    }

    @Test
    void orderEventMarksActorsOwnNotificationReadAndLeavesCounterpartyUnread() {
        OrderInfo order = new OrderInfo();
        order.setId(78L);
        order.setEmployerId(15L);
        order.setPainterId(2L);

        messageService.createOrderEventMessage(order, 15L, "ORDER_CANCELLED", "订单已取消");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        assertEquals(15L, captor.getAllValues().get(0).getToId());
        org.junit.jupiter.api.Assertions.assertNotNull(captor.getAllValues().get(0).getReadAt());
        assertEquals(2L, captor.getAllValues().get(1).getToId());
        org.junit.jupiter.api.Assertions.assertNull(captor.getAllValues().get(1).getReadAt());
        assertEquals(2L, captor.getAllValues().get(2).getToId());
    }

    @Test
    void adminOrderEventRemainsInsideTheTradeParticipantsConversation() {
        OrderInfo order = new OrderInfo();
        order.setId(78L);
        order.setEmployerId(15L);
        order.setPainterId(2L);

        messageService.createOrderEventMessage(order, 3L, "ORDER_ARBITRATION_REFUND", "管理员已退款");

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        Message conversationMessage = captor.getAllValues().get(2);
        assertEquals(2L, conversationMessage.getFromId());
        assertEquals(15L, conversationMessage.getToId());
        assertEquals("2_15", conversationMessage.getConversationKey());
    }

    @Test
    void conversationSummaryUsesConversationKeyForLegacyAdminAuthoredOrderEvent() {
        Message legacy = new Message();
        legacy.setId(397L);
        legacy.setFromId(3L);
        legacy.setToId(16L);
        legacy.setType(3);
        legacy.setConversationKey("2_16");
        legacy.setContent("管理员仲裁为全额结算画师");
        legacy.setCreatedAt(java.time.LocalDateTime.now());
        when(messageMapper.selectList(any())).thenReturn(List.of(legacy));
        when(userMapper.selectById(2L)).thenReturn(activeUser(2L));

        com.baomidou.mybatisplus.extension.plugins.pagination.Page<ConversationSummary> result =
                messageService.listConversations(16L, 1, 20);

        assertEquals(1, result.getRecords().size());
        assertEquals(2L, result.getRecords().get(0).getWithUserId());
        assertEquals(1, result.getRecords().get(0).getUnreadCount());
    }

    @Test
    void employerStartsProjectConversationForPendingApplicant() {
        Project project = ownedProject();
        when(projectMapper.selectById(8L)).thenReturn(project);
        when(applicationMapper.selectOne(any())).thenReturn(pendingApplication());

        messageService.startProjectConversation(1L, 8L, 2L);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        assertEquals(1L, captor.getValue().getFromId());
        assertEquals(2L, captor.getValue().getToId());
        assertEquals(8L, captor.getValue().getProjectId());
        assertEquals("APPLICATION_CONVERSATION_STARTED", captor.getValue().getSubtype());
        assertEquals("8:1_2:APPLICATION_CONVERSATION_STARTED", captor.getValue().getStarterKey());
    }

    @Test
    void employerStartsProjectConversationForSelectedApplicant() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        Application selected = pendingApplication();
        selected.setStatus(1);
        when(applicationMapper.selectOne(any())).thenReturn(selected);

        messageService.startProjectConversation(1L, 8L, 2L);

        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    void employerCannotStartProjectConversationForWithdrawnApplicant() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        Application withdrawn = pendingApplication();
        withdrawn.setStatus(3);
        when(applicationMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> messageService.startProjectConversation(1L, 8L, 2L));

        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void nonOwnerCannotStartProjectConversation() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());

        assertThrows(BusinessException.class, () -> messageService.startProjectConversation(3L, 8L, 2L));

        verify(applicationMapper, never()).selectOne(any());
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void messageServiceDoesNotExposeLegacyProjectConversationCreation() {
        assertThrows(NoSuchMethodException.class,
                () -> MessageService.class.getMethod("createProjectConversation", Long.class, Long.class, Long.class));
    }

    @Test
    void duplicateProjectConversationInitiationReturnsTheSingleStarterRecord() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        when(applicationMapper.selectOne(any())).thenReturn(pendingApplication());
        Message existing = new Message();
        existing.setId(99L);
        when(messageMapper.selectOne(any())).thenReturn(null, existing);
        org.mockito.Mockito.doThrow(new DuplicateKeyException("duplicate starter"))
                .when(messageMapper).insert(any(Message.class));

        Message started = messageService.startProjectConversation(1L, 8L, 2L);

        assertEquals(99L, started.getId());
        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    void legacyStarterRowPreventsDuplicateInitiationAfterUpgrade() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        when(applicationMapper.selectOne(any())).thenReturn(pendingApplication());
        Message legacyStarter = new Message();
        legacyStarter.setId(88L);
        legacyStarter.setStarterKey(null);
        when(messageMapper.selectOne(any())).thenReturn(legacyStarter);

        Message started = messageService.startProjectConversation(1L, 8L, 2L);

        assertEquals(88L, started.getId());
        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void applicantCannotSendProjectMessageBeforeEmployerStartsConversation() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L));
        when(applicationMapper.selectCount(any())).thenReturn(1L);
        when(messageMapper.selectOne(any())).thenReturn(null);

        assertThrows(BusinessException.class, () -> messageService.sendPrivateMessage(2L, request(1L, 8L, "reply")));

        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void applicantCanReplyAfterEmployerStartsProjectConversation() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L));
        when(applicationMapper.selectCount(any())).thenReturn(1L);
        Message starter = new Message();
        starter.setId(99L);
        when(messageMapper.selectOne(any())).thenReturn(starter);

        messageService.sendPrivateMessage(2L, request(1L, 8L, "reply"));

        verify(messageMapper).insert(any(Message.class));
    }

    @Test
    void applicantCanReplyAfterUpgradeFromLegacyStarterRow() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());
        when(userMapper.selectById(1L)).thenReturn(activeUser(1L));
        when(applicationMapper.selectCount(any())).thenReturn(1L);
        Message legacyStarter = new Message();
        legacyStarter.setId(88L);
        legacyStarter.setStarterKey(null);
        when(messageMapper.selectOne(any())).thenReturn(legacyStarter);

        messageService.sendPrivateMessage(2L, request(1L, 8L, "reply"));

        verify(messageMapper).insert(any(Message.class));
    }

    private PrivateMessageRequest request(Long toId, Long projectId, String content) {
        PrivateMessageRequest request = new PrivateMessageRequest();
        request.setToId(toId);
        request.setProjectId(projectId);
        request.setContent(content);
        return request;
    }

    private User activeUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setStatus(0);
        user.setDeleted(0);
        return user;
    }

    private Project ownedProject() {
        Project project = new Project();
        project.setId(8L);
        project.setEmployerId(1L);
        project.setDeleted(0);
        return project;
    }

    private Application pendingApplication() {
        Application application = new Application();
        application.setProjectId(8L);
        application.setPainterId(2L);
        application.setStatus(0);
        application.setDeleted(0);
        return application;
    }
}
