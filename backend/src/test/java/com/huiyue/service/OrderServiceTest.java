package com.huiyue.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.huiyue.common.BusinessException;
import com.huiyue.dto.OrderStageReviewRequest;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.OrderStage;
import com.huiyue.entity.Stall;
import com.huiyue.entity.User;
import com.huiyue.mapper.OrderEventMapper;
import com.huiyue.mapper.OrderInfoMapper;
import com.huiyue.mapper.OrderStageMapper;
import com.huiyue.mapper.StallMapper;
import com.huiyue.mapper.UserMapper;
import com.huiyue.service.impl.OrderServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private OrderStageMapper orderStageMapper;
    @Mock private OrderEventMapper orderEventMapper;
    @Mock private StallMapper stallMapper;
    @Mock private UserMapper userMapper;
    @Mock private MessageService messageService;
    @Mock private OrderPreviewService orderPreviewService;
    @Spy private ObjectMapper objectMapper = new ObjectMapper();
    @InjectMocks private OrderServiceImpl orderService;

    @Test
    void createStallPurchaseLocksBalanceAndDecrementsStock() {
        Stall stall = activeStall();
        when(stallMapper.selectByIdForUpdate(9L)).thenReturn(stall);
        User employer = activeUser(15L, new BigDecimal("1000.00"));
        User painter = activeUser(2L, BigDecimal.ZERO);
        when(userMapper.selectByIdForUpdate(15L)).thenReturn(employer);
        when(userMapper.selectById(2L)).thenReturn(painter);
        org.mockito.Mockito.doAnswer(invocation -> {
            OrderInfo order = invocation.getArgument(0);
            order.setId(77L);
            return 1;
        }).when(orderInfoMapper).insert(any(OrderInfo.class));

        OrderInfo result = orderService.createStallPurchase(9L, 15L);

        assertEquals(new BigDecimal("712.00"), employer.getBalance());
        assertEquals(2, stall.getStock());
        assertEquals(77L, result.getId());
        assertEquals("角色立绘委托", result.getStallTitle());
        verify(userMapper).updateById(employer);
        verify(stallMapper).updateById(stall);
        verify(messageService).createStallPurchaseCommunication(eq(result), eq(stall));
    }

    @Test
    void getDetailRejectsUserOutsideParticipants() {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setDeleted(0);
        when(orderInfoMapper.selectById(77L)).thenReturn(order);

        assertThrows(com.huiyue.common.BusinessException.class,
                () -> orderService.getDetail(77L, 99L));
    }

    @Test
    void acceptRequiresAssignedCertifiedPainter() {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setStatus(0);
        order.setDeleted(0);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        User painter = activeUser(2L, BigDecimal.ZERO);
        painter.setCertStatus(2);
        when(userMapper.selectById(2L)).thenReturn(painter);

        OrderInfo result = orderService.accept(77L, 2L);

        assertEquals(1, result.getStatus());
        verify(orderInfoMapper).updateById(result);
        verify(messageService).createOrderEventMessage(eq(result), eq(2L), eq("ORDER_ACCEPTED"), any());
    }

    @Test
    void painterCannotRequestAfterSale() {
        OrderInfo order = activeOrder(1);
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);

        assertThrows(BusinessException.class,
                () -> orderService.requestAfterSale(77L, 2L, "画师申请售后"));
    }

    @Test
    void employerCanCompleteFromFirstSubmittedStageAndPainterIsPaidImmediately() {
        OrderInfo order = activeOrder(2);
        order.setAmount(new BigDecimal("288.00"));
        order.setCurrentStage(0);
        order.setPhaseSnapshot("[{\"name\":\"初稿\",\"percent\":40},{\"name\":\"终稿\",\"percent\":60}]");
        OrderStage stage = new OrderStage();
        stage.setId(8L);
        stage.setOrderId(77L);
        stage.setStageIndex(0);
        stage.setSubmissionNo(1);
        stage.setName("初稿");
        stage.setStatus(0);
        User painter = activeUser(2L, new BigDecimal("10.00"));
        when(orderInfoMapper.selectByIdForUpdate(77L)).thenReturn(order);
        when(orderStageMapper.selectList(any())).thenReturn(List.of(stage));
        when(userMapper.selectByIdForUpdate(2L)).thenReturn(painter);
        OrderStageReviewRequest request = new OrderStageReviewRequest();
        request.setAction("complete");

        OrderInfo result = orderService.reviewStage(77L, 15L, request);

        assertEquals(3, result.getStatus());
        assertEquals(new BigDecimal("298.00"), painter.getBalance());
        assertEquals(1, stage.getStatus());
        verify(userMapper).updateById(painter);
        verify(messageService).createOrderEventMessage(eq(result), eq(15L), eq("ORDER_COMPLETED"), any());
    }

    private Stall activeStall() {
        Stall stall = new Stall();
        stall.setId(9L);
        stall.setPainterId(2L);
        stall.setTitle("角色立绘委托");
        stall.setCover("/uploads/cover.png");
        stall.setPrice(new BigDecimal("288.00"));
        stall.setStock(3);
        stall.setStatus(1);
        stall.setDeleted(0);
        stall.setPhases("[{\"name\":\"初稿\",\"percent\":50},{\"name\":\"交付\",\"percent\":50}]");
        return stall;
    }

    private User activeUser(Long id, BigDecimal balance) {
        User user = new User();
        user.setId(id);
        user.setUsername("user" + id);
        user.setBalance(balance);
        user.setStatus(0);
        user.setDeleted(0);
        return user;
    }

    private OrderInfo activeOrder(int status) {
        OrderInfo order = new OrderInfo();
        order.setId(77L);
        order.setEmployerId(15L);
        order.setPainterId(2L);
        order.setStatus(status);
        order.setCurrentStage(0);
        order.setDeleted(0);
        return order;
    }
}
