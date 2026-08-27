package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.entity.Work;
import com.artiv.dto.WorkFavoriteCount;
import com.artiv.mapper.WorkFavoriteMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.WorkServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkServiceTest {

    @Mock
    private WorkMapper workMapper;

    @Mock
    private WorkFavoriteMapper workFavoriteMapper;

    @InjectMocks
    private WorkServiceImpl workService;

    @Test
    void publicListAcceptsGalleryFiltersAndReturnsMapperPage() {
        Page<Work> expected = new Page<>(1, 12);
        when(workMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        Page<Work> result = workService.listWorks(1, 12, null, "立绘", "女性", "角色", "oldest");

        verify(workMapper).selectPage(any(Page.class), any());
        assertEquals(expected, result);
    }

    @Test
    void popularListUsesFavoriteAggregationQuery() {
        Page<Work> expected = new Page<>(1, 12);
        when(workMapper.selectPopularPage(any(Page.class), any(), any(), any())).thenReturn(expected);

        Page<Work> result = workService.listWorks(1, 12, null, "立绘", "女性,日系", "角色", "popular");

        verify(workMapper).selectPopularPage(any(Page.class), any(), any(), any());
        assertEquals(expected, result);
    }

    @Test
    void followingListUsesFollowRelationQueryAndKeepsGalleryFilters() {
        Page<Work> expected = new Page<>(1, 12);
        when(workMapper.selectFollowingPage(any(Page.class), any(), any(), any(), any())).thenReturn(expected);

        Page<Work> result = workService.listFollowingWorks(1, 12, 19L, "立绘", "女性,日系", "角色");

        verify(workMapper).selectFollowingPage(any(Page.class), any(), any(), any(), any());
        assertEquals(expected, result);
    }

    @Test
    void publicListFillsFavoriteCountAndCurrentUserStateInBatch() {
        Work work = new Work();
        work.setId(6L);
        Page<Work> expected = new Page<>(1, 12);
        expected.setRecords(java.util.List.of(work));
        when(workMapper.selectPage(any(Page.class), any())).thenReturn(expected);
        WorkFavoriteCount count = new WorkFavoriteCount();
        count.setWorkId(6L);
        count.setFavoriteCount(4L);
        when(workFavoriteMapper.selectCounts(java.util.List.of(6L))).thenReturn(java.util.List.of(count));
        when(workFavoriteMapper.selectFavoritedWorkIds(eq(19L), eq(java.util.List.of(6L))))
                .thenReturn(java.util.List.of(6L));

        Page<Work> result = workService.listWorks(1, 12, 19L, null, null, null, "newest");

        assertEquals(4L, result.getRecords().get(0).getFavoriteCount());
        assertEquals(true, result.getRecords().get(0).getFavorited());
    }

    @Test
    void createDefaultsVisibilityToPublicForCompatibility() {
        Work work = new Work();
        work.setTitle("新作品");
        work.setCategory("立绘");
        work.setSubject("女性");
        work.setImage("/uploads/work.png");

        workService.create(work, 8L);

        ArgumentCaptor<Work> workCaptor = ArgumentCaptor.forClass(Work.class);
        verify(workMapper).insert(workCaptor.capture());
        Work saved = workCaptor.getValue();
        assertEquals(8L, saved.getUserId());
        assertEquals(0, saved.getDeleted());
        assertEquals(1, saved.getVisibility());
    }

    @Test
    void ownerCanToggleWorkVisibility() {
        Work work = new Work();
        work.setId(12L);
        work.setUserId(8L);
        work.setVisibility(1);
        when(workMapper.selectById(12L)).thenReturn(work);

        Work updated = workService.changeVisibility(12L, 0, 8L);

        verify(workMapper).updateById(work);
        assertEquals(0, updated.getVisibility());
        assertNotNull(updated.getUpdatedAt());
    }
}
