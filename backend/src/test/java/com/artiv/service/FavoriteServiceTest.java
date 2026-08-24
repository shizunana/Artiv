package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.dto.FavoriteStallItem;
import com.artiv.dto.StallSalesCount;
import com.artiv.entity.Stall;
import com.artiv.entity.StallFavorite;
import com.artiv.entity.Work;
import com.artiv.entity.WorkFavorite;
import com.artiv.mapper.StallFavoriteMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.WorkFavoriteMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.FavoriteServiceImpl;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    @Mock private WorkFavoriteMapper workFavoriteMapper;
    @Mock private StallFavoriteMapper stallFavoriteMapper;
    @Mock private WorkMapper workMapper;
    @Mock private StallMapper stallMapper;
    @Mock private OrderInfoMapper orderInfoMapper;
    @Mock private AccountRestrictionService accountRestrictionService;

    @InjectMocks private FavoriteServiceImpl favoriteService;

    @Test
    void favoritePublicWorkCreatesRelation() {
        when(workMapper.selectById(8L)).thenReturn(work(8L, 16L, 1));
        when(workFavoriteMapper.selectCount(any())).thenReturn(0L);

        var state = favoriteService.favoriteWork(8L, 3L);

        assertTrue(state.isFavorited());
        ArgumentCaptor<WorkFavorite> captor = ArgumentCaptor.forClass(WorkFavorite.class);
        verify(workFavoriteMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals(8L, captor.getValue().getWorkId());
    }

    @Test
    void favoriteWorkRejectsPrivateTarget() {
        when(workMapper.selectById(8L)).thenReturn(work(8L, 16L, 0));

        assertThrows(BusinessException.class, () -> favoriteService.favoriteWork(8L, 3L));
        verify(workFavoriteMapper, never()).insert(any(WorkFavorite.class));
    }

    @Test
    void favoriteWorkRejectsOwnTarget() {
        when(workMapper.selectById(8L)).thenReturn(work(8L, 3L, 1));

        assertThrows(BusinessException.class, () -> favoriteService.favoriteWork(8L, 3L));
        verify(workFavoriteMapper, never()).insert(any(WorkFavorite.class));
    }

    @Test
    void favoriteListedStallCreatesRelation() {
        when(stallMapper.selectById(9L)).thenReturn(stall(9L, 16L, 1));
        when(stallFavoriteMapper.selectCount(any())).thenReturn(0L);

        var state = favoriteService.favoriteStall(9L, 3L);

        assertTrue(state.isFavorited());
        ArgumentCaptor<StallFavorite> captor = ArgumentCaptor.forClass(StallFavorite.class);
        verify(stallFavoriteMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals(9L, captor.getValue().getStallId());
    }

    @Test
    void favoriteStallRejectsOffShelfTarget() {
        when(stallMapper.selectById(9L)).thenReturn(stall(9L, 16L, 0));

        assertThrows(BusinessException.class, () -> favoriteService.favoriteStall(9L, 3L));
        verify(stallFavoriteMapper, never()).insert(any(StallFavorite.class));
    }

    @Test
    void unfavoriteRemainsAvailableForRestrictedAccounts() {
        var workState = favoriteService.unfavoriteWork(8L, 3L);
        var stallState = favoriteService.unfavoriteStall(9L, 3L);

        assertFalse(workState.isFavorited());
        assertFalse(stallState.isFavorited());
        verify(workFavoriteMapper).delete(any());
        verify(stallFavoriteMapper).delete(any());
        verify(accountRestrictionService, never()).requireAvailable(any(), any());
    }

    @Test
    void listFavoriteStallsKeepsFiltersAndFillsCompletedSalesInBatch() {
        FavoriteStallItem item = new FavoriteStallItem();
        item.setStallId(9L);
        Page<FavoriteStallItem> expected = new Page<>(1, 20);
        expected.setRecords(List.of(item));
        when(stallFavoriteMapper.selectMine(any(Page.class), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(expected);
        StallSalesCount salesCount = new StallSalesCount();
        salesCount.setStallId(9L);
        salesCount.setSoldCount(4L);
        when(orderInfoMapper.selectCompletedSalesByStallIds(List.of(9L)))
                .thenReturn(List.of(salesCount));

        Page<FavoriteStallItem> result = favoriteService.listStalls(
                1, 20, 3L, "头像", "关键词", "priceAsc", "日系",
                new BigDecimal("100"), new BigDecimal("500"), "24小时");

        assertEquals(4L, result.getRecords().get(0).getSoldCount());
        verify(orderInfoMapper).selectCompletedSalesByStallIds(List.of(9L));
    }

    private Work work(Long id, Long userId, int visibility) {
        Work work = new Work();
        work.setId(id);
        work.setUserId(userId);
        work.setVisibility(visibility);
        work.setDeleted(0);
        return work;
    }

    private Stall stall(Long id, Long painterId, int status) {
        Stall stall = new Stall();
        stall.setId(id);
        stall.setPainterId(painterId);
        stall.setStatus(status);
        stall.setDeleted(0);
        return stall;
    }
}
