package com.huiyue.service;

import com.huiyue.common.BusinessException;
import com.huiyue.dto.PrivateMessageRequest;
import com.huiyue.entity.Message;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;
import com.huiyue.entity.User;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private ProjectMapper projectMapper;

    @InjectMocks
    private MessageServiceImpl messageService;

    @Test
    void sendPrivateMessageUsesCurrentUserAndNormalizedConversationKey() {
        User recipient = new User();
        recipient.setId(2L);
        recipient.setStatus(0);
        recipient.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(recipient);

        PrivateMessageRequest request = new PrivateMessageRequest();
        request.setToId(2L);
        request.setContent("你好，想沟通一下企划细节。");

        messageService.sendPrivateMessage(15L, request);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(captor.capture());
        Message saved = captor.getValue();
        assertEquals(15L, saved.getFromId());
        assertEquals(2L, saved.getToId());
        assertEquals("2_15", saved.getConversationKey());
        assertEquals(3, saved.getType());
        assertEquals("PRIVATE_CHAT", saved.getSubtype());
    }

    @Test
    void sendPrivateMessageRejectsSendingToSelf() {
        PrivateMessageRequest request = new PrivateMessageRequest();
        request.setToId(15L);
        request.setContent("给自己发消息");

        assertThrows(BusinessException.class, () -> messageService.sendPrivateMessage(15L, request));

        verify(messageMapper, never()).insert(any(Message.class));
    }

    @Test
    void createStallPurchaseCommunicationCreatesTwoNotificationsAndOneTradeConversation() {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);

        Stall stall = new Stall();
        stall.setId(9L);
        stall.setTitle("角色立绘委托");

        messageService.createStallPurchaseCommunication(order, stall);

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper, org.mockito.Mockito.times(3)).insert(captor.capture());
        var created = captor.getAllValues();
        assertEquals(15L, created.get(0).getToId());
        assertEquals(0, created.get(0).getType());
        assertEquals("STALL_PURCHASED", created.get(0).getSubtype());
        assertEquals(2L, created.get(1).getToId());
        assertEquals("STALL_PURCHASE_RECEIVED", created.get(1).getSubtype());
        assertEquals(15L, created.get(2).getFromId());
        assertEquals(2L, created.get(2).getToId());
        assertEquals(3, created.get(2).getType());
        assertEquals("STALL_PURCHASE_CONVERSATION_STARTED", created.get(2).getSubtype());
        assertEquals("2_15", created.get(2).getConversationKey());
        assertEquals(77L, created.get(2).getOrderId());
    }
}
