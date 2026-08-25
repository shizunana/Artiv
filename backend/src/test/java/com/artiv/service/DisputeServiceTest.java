package com.artiv.service;

import com.artiv.dto.admin.DisputeCreateRequest;
import com.artiv.entity.OrderDispute;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.OrderDisputeEvidence;
import com.artiv.mapper.OrderDisputeEvidenceMapper;
import com.artiv.mapper.OrderDisputeMapper;
import com.artiv.mapper.OrderEventMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.DisputeServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DisputeServiceTest {
    @Mock OrderDisputeMapper disputeMapper;
    @Mock OrderDisputeEvidenceMapper evidenceMapper;
    @Mock OrderInfoMapper orderInfoMapper;
    @Mock OrderEventMapper orderEventMapper;
    @Mock UserMapper userMapper;
    @Mock OrderService orderService;
    @Mock MessageService messageService;
    @Mock AdminAuditService auditService;
    @InjectMocks DisputeServiceImpl disputeService;

    @Test
    void painterCanOpenDisputeAndSubmitImages() {
        OrderInfo order = new OrderInfo();
        order.setId(19L);
        order.setEmployerId(1L);
        order.setPainterId(2L);
        order.setProjectId(8L);
        order.setProjectTitle("角色立绘企划");
        order.setStatus(1);
        order.setDeleted(0);
        when(orderInfoMapper.selectByIdForUpdate(19L)).thenReturn(order);
        when(disputeMapper.selectCount(any())).thenReturn(0L);
        doAnswer(invocation -> {
            OrderDispute dispute = invocation.getArgument(0);
            dispute.setId(7L);
            return 1;
        }).when(disputeMapper).insert(any(OrderDispute.class));

        DisputeCreateRequest request = new DisputeCreateRequest();
        request.setReason("阶段稿与约定不一致");
        request.setImages(List.of("/uploads/evidence-a.png", "/uploads/evidence-b.png"));
        OrderDispute dispute = disputeService.create(19L, 2L, request);

        assertEquals(7L, dispute.getId());
        assertEquals(5, order.getStatus());
        verify(evidenceMapper, org.mockito.Mockito.times(2)).insert(any(OrderDisputeEvidence.class));
        verify(messageService).createOrderEventMessage(eq(order), eq(2L), eq("ORDER_DISPUTE_CREATED"),
                eq("订单「角色立绘企划」已发起争议并等待管理员仲裁。原因：阶段稿与约定不一致"));
    }
}
