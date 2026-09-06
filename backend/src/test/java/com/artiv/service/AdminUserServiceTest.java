package com.artiv.service;

import com.artiv.dto.admin.AdminUserStatusRequest;
import com.artiv.dto.admin.AdminBalanceAdjustmentRequest;
import com.artiv.dto.admin.AdminBalanceSetRequest;
import com.artiv.entity.OrderDispute;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.User;
import com.artiv.common.BusinessException;
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
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
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

    @Test
    void creditBalanceUsesLockedBalanceAndWritesAuditSnapshot() {
        User user = user(1L, "100.00");
        when(userMapper.selectByIdForUpdate(1L)).thenReturn(user);
        when(userMapper.incrementBalance(1L, new BigDecimal("250.00"))).thenReturn(1);
        AdminBalanceAdjustmentRequest request = adjustment("250.00");

        var result = adminUserService.creditBalance(3L, 1L, request);

        assertEquals(new BigDecimal("350.00"), result.get("balance"));
        assertEquals("CREDIT", result.get("type"));
        verify(userMapper).incrementBalance(1L, new BigDecimal("250.00"));
        verify(auditService).record(eq(3L), eq("CREDIT_USER_BALANCE"), eq("USER"), eq(1L),
                eq(java.util.Map.of("balance", new BigDecimal("100.00"))),
                eq(java.util.Map.of("balance", new BigDecimal("350.00"), "amount", new BigDecimal("250.00"))), eq("功能测试"));
    }

    @Test
    void adjustBalanceSetsExactTargetWithPositiveOrNegativeDelta() {
        User enough = user(1L, "100.00");
        when(userMapper.selectByIdForUpdate(1L)).thenReturn(enough);
        when(userMapper.incrementBalance(1L, new BigDecimal("-40.00"))).thenReturn(1);
        var result = adminUserService.adjustBalance(3L, 1L, balanceSet("60.00"));
        assertEquals(new BigDecimal("60.00"), result.get("balance"));
        assertEquals(new BigDecimal("-40.00"), result.get("delta"));
        assertEquals("ADJUST", result.get("type"));
        verify(auditService).record(eq(3L), eq("ADJUST_USER_BALANCE"), eq("USER"), eq(1L),
                eq(java.util.Map.of("balance", new BigDecimal("100.00"))),
                eq(java.util.Map.of("balance", new BigDecimal("60.00"), "delta", new BigDecimal("-40.00"))), eq("功能测试"));

        User lower = user(2L, "5.00");
        when(userMapper.selectByIdForUpdate(2L)).thenReturn(lower);
        when(userMapper.incrementBalance(2L, new BigDecimal("10.00"))).thenReturn(1);
        assertEquals(new BigDecimal("15.00"), adminUserService.adjustBalance(3L, 2L, balanceSet("15.00")).get("balance"));
    }

    @Test
    void balanceAdjustmentRejectsInvalidAmountOrDeletedTargetBeforeUpdate() {
        var invalid = adjustment("0.001");
        assertEquals(2001, assertThrows(BusinessException.class,
                () -> adminUserService.creditBalance(3L, 1L, invalid)).getCode());
        assertEquals(2001, assertThrows(BusinessException.class,
                () -> adminUserService.adjustBalance(3L, 1L, balanceSet("-0.01"))).getCode());
        User deleted = user(1L, "100.00"); deleted.setDeleted(1);
        when(userMapper.selectByIdForUpdate(1L)).thenReturn(deleted);
        assertEquals(2002, assertThrows(BusinessException.class,
                () -> adminUserService.creditBalance(3L, 1L, adjustment("10.00"))).getCode());
        verify(userMapper, never()).incrementBalance(any(), any());
    }

    private User user(Long id, String balance) {
        User user = new User(); user.setId(id); user.setDeleted(0); user.setBalance(new BigDecimal(balance)); return user;
    }

    private AdminBalanceAdjustmentRequest adjustment(String amount) {
        var request = new AdminBalanceAdjustmentRequest();
        request.setAmount(new BigDecimal(amount)); request.setReason("功能测试"); return request;
    }

    private AdminBalanceSetRequest balanceSet(String balance) {
        var request = new AdminBalanceSetRequest();
        request.setBalance(new BigDecimal(balance)); request.setReason("功能测试"); return request;
    }
}
