package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Work;
import com.huiyue.mapper.WorkMapper;
import com.huiyue.service.impl.WorkServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkServiceTest {

    @Mock
    private WorkMapper workMapper;

    @InjectMocks
    private WorkServiceImpl workService;

    @Test
    void publicListAcceptsGalleryFiltersAndReturnsMapperPage() {
        Page<Work> expected = new Page<>(1, 12);
        when(workMapper.selectPage(any(Page.class), any())).thenReturn(expected);

        Page<Work> result = workService.listWorks(1, 12, "立绘", "女性", "角色", "oldest");

        verify(workMapper).selectPage(any(Page.class), any());
        assertEquals(expected, result);
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
}
