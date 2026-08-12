package com.huiyue.service;

import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;
import com.huiyue.entity.User;
import com.huiyue.mapper.OrderInfoMapper;
import com.huiyue.mapper.StallMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private StallMapper stallMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    void createStallPurchaseCreatesCommunicationOrderAndNotifiesBothSides() {
        Stall stall = new Stall();
        stall.setId(9L);
        stall.setPainterId(2L);
        stall.setTitle("角色立绘委托");
        stall.setPrice(new BigDecimal("288.00"));
        stall.setStock(3);
        stall.setStatus(1);
        stall.setDeleted(0);
        when(stallMapper.selectById(9L)).thenReturn(stall);

        User painter = new User();
        painter.setId(2L);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectById(2L)).thenReturn(painter);

        doAssignIdOnInsert();

        orderService.createStallPurchase(9L, 15L);

        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).insert(orderCaptor.capture());
        OrderInfo saved = orderCaptor.getValue();
        assertEquals(9L, saved.getStallId());
        assertEquals(15L, saved.getEmployerId());
        assertEquals(2L, saved.getPainterId());
        assertEquals(new BigDecimal("288.00"), saved.getAmount());
        assertEquals(0, saved.getStatus());
        assertEquals(3, stall.getStock());
        verify(stallMapper, org.mockito.Mockito.never()).updateById(stall);
        verify(messageService).createStallPurchaseCommunication(eq(saved), eq(stall));
    }

    private void doAssignIdOnInsert() {
        org.mockito.Mockito.doAnswer(invocation -> {
            OrderInfo order = invocation.getArgument(0);
            order.setId(77L);
            return 1;
        }).when(orderInfoMapper).insert(any(OrderInfo.class));
    }
}
