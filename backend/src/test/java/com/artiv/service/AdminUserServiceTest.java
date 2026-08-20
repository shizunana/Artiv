package com.artiv.service;

import com.artiv.dto.admin.AdminUserStatusRequest;
import com.artiv.entity.OrderDispute;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.User;
import com.artiv.mapper.ArtistCertificationMapper;
import com.artiv.mapper.OrderDisputeMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.AdminUserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private ArtistCertificationMapper certificationMapper;
    @Mock private OrderDisputeMapper disputeMapper;
    @Mock private OrderService orderService;
    @Mock private MessageService messageService;
    @Mock private AdminAuditService auditService;
    @InjectMocks private AdminUserServiceImpl adminUserService;

    @Test
    void banRefundsOrdersAndClosesTheirPendingDisputes() {
        User painter = new User();
        painter.setId(2L);
        painter.setRole(2);
        painter.setStatus(0);
        painter.setDeleted(0);
        when(userMapper.selectByIdForUpdate(2L)).thenReturn(painter);
        when(orderService.cancelUnfinishedForBannedUser(eq(2L), eq(3L), any())).thenReturn(1);

        OrderDispute dispute = new OrderDispute();
        dispute.setId(17L);
        dispute.setOrderId(41L);
        dispute.setStatus(0);
        when(disputeMapper.selectList(any())).thenReturn(List.of(dispute));
        OrderInfo refundedOrder = new OrderInfo();
        refundedOrder.setId(41L);
        refundedOrder.setEmployerId(15L);
        refundedOrder.setPainterId(2L);
        refundedOrder.setStatus(4);
        when(orderInfoMapper.selectById(41L)).thenReturn(refundedOrder);

        AdminUserStatusRequest request = new AdminUserStatusRequest();
        request.setStatus(1);
        request.setReason("严重违规");

        var response = adminUserService.changeStatus(3L, 2L, request);

        assertEquals(1, response.get("cancelledOrders"));
        assertEquals(1, painter.getStatus());
        ArgumentCaptor<OrderDispute> captor = ArgumentCaptor.forClass(OrderDispute.class);
        verify(disputeMapper).updateById(captor.capture());
        assertEquals(1, captor.getValue().getStatus());
        assertEquals(1, captor.getValue().getResult());
        verify(auditService).record(eq(3L), eq("RESOLVE_DISPUTE_ON_BAN"),
                eq("ORDER_DISPUTE"), eq(17L), any(), any(), eq("严重违规"));
    }
}
