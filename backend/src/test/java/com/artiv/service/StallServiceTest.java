package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.artiv.dto.StallLimitedUpdateRequest;
import com.artiv.dto.StallSalesCount;
import com.artiv.entity.Stall;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.StallServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StallServiceTest {

    @Mock
    private StallMapper stallMapper;

    @Mock
    private UserMapper userMapper;

    @Mock
    private OrderInfoMapper orderInfoMapper;

    @Mock
    private AccountRestrictionService accountRestrictionService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private StallServiceImpl stallService;

    @Test
    void deleteTakesStallOfflineBeforeLogicalDeletion() {
        Stall stall = new Stall();
        stall.setId(27L);
        stall.setPainterId(2L);
        stall.setStatus(1);
        stall.setDeleted(0);
        when(stallMapper.selectById(27L)).thenReturn(stall);

        stallService.delete(27L, 2L);

        InOrder operations = inOrder(stallMapper);
        operations.verify(stallMapper).selectById(27L);
        operations.verify(stallMapper).updateById(stall);
        operations.verify(stallMapper).deleteById(27L);
        assertEquals(0, stall.getStatus());
        assertNotNull(stall.getUpdatedAt());
    }

    @Test
    void deleteRejectsStallWithUnfinishedOrders() {
        Stall stall = ownedStall();
        when(stallMapper.selectById(27L)).thenReturn(stall);
        when(orderInfoMapper.selectCount(any())).thenReturn(1L);

        com.artiv.common.BusinessException exception = assertThrows(
                com.artiv.common.BusinessException.class,
                () -> stallService.delete(27L, 2L));

        assertEquals("该橱窗存在未完成订单，暂时无法删除", exception.getMessage());
        verify(stallMapper, never()).updateById(any(Stall.class));
        verify(stallMapper, never()).deleteById(any(Long.class));
    }

    @Test
    void ownerCanViewOfflineOutOfStockStall() {
        Stall stall = ownedStall();
        stall.setStatus(0);
        stall.setStock(0);
        when(stallMapper.selectById(27L)).thenReturn(stall);

        Stall result = stallService.getById(27L, 2L);

        assertEquals(stall, result);
        assertEquals(false, result.getHasUnfinishedOrders());
    }

    @Test
    void createRejectsCategoryOutsideSharedOptions() {
        Stall stall = ownedStall();
        stall.setCategory("国潮插画");

        com.artiv.common.BusinessException exception = assertThrows(
                com.artiv.common.BusinessException.class,
                () -> stallService.create(stall, 2L));

        assertEquals("橱窗类型必须从统一选项中选择", exception.getMessage());
        verify(stallMapper, never()).insert(any(Stall.class));
    }

    @Test
    void anonymousUserCannotViewOutOfStockStall() {
        Stall stall = ownedStall();
        stall.setStock(0);
        when(stallMapper.selectById(27L)).thenReturn(stall);

        assertThrows(com.artiv.common.BusinessException.class,
                () -> stallService.getById(27L, null));
    }

    @Test
    void historicalBuyerCanViewOfflineOutOfStockStall() {
        Stall stall = ownedStall();
        stall.setStatus(0);
        stall.setStock(0);
        when(stallMapper.selectById(27L)).thenReturn(stall);
        when(orderInfoMapper.selectCount(any())).thenReturn(1L);

        Stall result = stallService.getById(27L, 9L);

        assertEquals(stall, result);
    }

    @Test
    void limitedUpdateChangesStockAndOnlyAddsSubjects() {
        Stall stall = ownedStall();
        stall.setParams("{\"colorMode\":\"RGB\",\"subject\":[\"Q版\"]}");
        when(stallMapper.selectById(27L)).thenReturn(stall);
        StallLimitedUpdateRequest request = new StallLimitedUpdateRequest();
        request.setStock(8);
        request.setSubjects(List.of("Q版", "古风"));

        Stall result = stallService.updateLimited(27L, request, 2L);

        assertEquals(8, result.getStock());
        verify(stallMapper).updateById(stall);
        org.junit.jupiter.api.Assertions.assertTrue(result.getParams().contains("Q版"));
        org.junit.jupiter.api.Assertions.assertTrue(result.getParams().contains("古风"));
    }

    @Test
    void publicListUsesOneAggregateQueryAndFillsZeroForUnsoldStalls() {
        Stall sold = ownedStall();
        Stall unsold = ownedStall();
        unsold.setId(28L);
        Page<Stall> page = new Page<>(1, 20);
        page.setRecords(List.of(sold, unsold));
        when(stallMapper.selectPage(any(), any())).thenReturn(page);
        StallSalesCount sales = new StallSalesCount();
        sales.setStallId(27L);
        sales.setSoldCount(3L);
        when(orderInfoMapper.selectCompletedSalesByStallIds(List.of(27L, 28L))).thenReturn(List.of(sales));

        Page<Stall> result = stallService.listStalls(1, 20, null, null, null,
                null, null, null, null);

        assertEquals(3L, result.getRecords().get(0).getSoldCount());
        assertEquals(0L, result.getRecords().get(1).getSoldCount());
        verify(orderInfoMapper).selectCompletedSalesByStallIds(List.of(27L, 28L));
    }

    private Stall ownedStall() {
        Stall stall = new Stall();
        stall.setId(27L);
        stall.setPainterId(2L);
        stall.setStatus(1);
        stall.setStock(1);
        stall.setDeleted(0);
        return stall;
    }
}
