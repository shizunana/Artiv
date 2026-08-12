package com.huiyue.service;

import com.huiyue.common.BusinessException;
import com.huiyue.dto.PrivateMessageRequest;
import com.huiyue.entity.Application;
import com.huiyue.entity.Message;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Project;
import com.huiyue.entity.Stall;
import com.huiyue.entity.User;
import com.huiyue.mapper.ApplicationMapper;
import com.huiyue.mapper.MessageMapper;
import com.huiyue.mapper.ProjectMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.impl.MessageServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

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
    void orderIntentRejectsNonOwnerWithoutMutation() {
        when(projectMapper.selectById(8L)).thenReturn(ownedProject());

        assertThrows(BusinessException.class, () -> messageService.createProjectOrderIntent(3L, 8L, 2L));

        verify(applicationMapper, never()).selectOne(any());
        verify(messageMapper, never()).insert(any(Message.class));
        verify(projectMapper, never()).updateById(any(Project.class));
        verify(applicationMapper, never()).updateById(any(Application.class));
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
