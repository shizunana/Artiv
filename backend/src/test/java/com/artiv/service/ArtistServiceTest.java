package com.artiv.service;

import com.artiv.dto.ArtistSummary;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.ArtistServiceImpl;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private WorkMapper workMapper;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ArtistServiceImpl artistService;

    @Test
    void certifiedPaintersRemainVisibleWithoutWorks() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                painter(14L, "测试画师1", "日系,立绘,女性"),
                painter(15L, "测试画师2", "Q版,场景,可爱")
        ));
        when(workMapper.selectList(any())).thenReturn(List.of());

        Page<ArtistSummary> page = artistService.listArtists(1, 10, null, null, null);
        List<ArtistSummary> artists = page.getRecords();

        assertEquals(2, artists.size());
        assertEquals(2, page.getTotal());
        assertEquals("测试画师1", artists.get(0).getNickname());
        assertEquals(List.of(), artists.get(0).getWorks());
    }

    @Test
    void artistHallUsesCertificationInsteadOfRegistrationRole() {
        TableInfoHelper.initTableInfo(new MapperBuilderAssistant(new MybatisConfiguration(), ""), User.class);
        User certifiedPainter = painter(16L, "认证企划方主身份", "日系");
        certifiedPainter.setRole(1);
        when(userMapper.selectList(any())).thenReturn(List.of(certifiedPainter));
        when(workMapper.selectList(any())).thenReturn(List.of());

        Page<ArtistSummary> page = artistService.listArtists(1, 10, null, null, null);

        assertEquals(List.of(16L), page.getRecords().stream().map(ArtistSummary::getId).toList());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Wrapper<User>> queryCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(userMapper).selectList(queryCaptor.capture());
        String sql = queryCaptor.getValue().getCustomSqlSegment();
        assertTrue(sql.contains("cert_status"));
        assertFalse(sql.contains("role"));
    }

    @Test
    void categorySubjectAndKeywordFiltersUseRealProfileAndWorks() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                painter(2L, "画师阿绘", "LOGO,安头,定制"),
                painter(14L, "测试画师1", "日系,立绘,女性")
        ));
        Work work = new Work();
        work.setId(8L);
        work.setUserId(14L);
        work.setCategory("立绘");
        work.setImage("/uploads/test.png");
        when(workMapper.selectList(any())).thenReturn(List.of(work));

        List<ArtistSummary> artists = artistService
                .listArtists(1, 10, "立绘", "日系,女性", "测试画师")
                .getRecords();

        assertEquals(1, artists.size());
        assertEquals(14L, artists.get(0).getId());
        assertEquals(List.of("立绘"), artists.get(0).getCategories());
        assertEquals(1, artists.get(0).getWorks().size());
    }

    @Test
    void artistResultsAreReturnedInTenItemPages() {
        List<User> painters = java.util.stream.LongStream.rangeClosed(1, 12)
                .mapToObj(id -> painter(id, "测试画师" + id, "日系"))
                .toList();
        when(userMapper.selectList(any())).thenReturn(painters);
        when(workMapper.selectList(any())).thenReturn(List.of());

        Page<ArtistSummary> secondPage = artistService.listArtists(2, 10, null, null, null);

        assertEquals(12, secondPage.getTotal());
        assertEquals(2, secondPage.getRecords().size());
        assertEquals("测试画师11", secondPage.getRecords().get(0).getNickname());
    }

    @Test
    void extremePageNumberReturnsAnEmptyPageInsteadOfOverflowing() {
        when(userMapper.selectList(any())).thenReturn(List.of(painter(1L, "测试画师1", "日系")));
        when(workMapper.selectList(any())).thenReturn(List.of());

        Page<ArtistSummary> page = artistService.listArtists(Long.MAX_VALUE, 50, null, null, null);

        assertEquals(1, page.getTotal());
        assertEquals(List.of(), page.getRecords());
    }

    private User painter(Long id, String nickname, String tags) {
        User user = new User();
        user.setId(id);
        user.setUsername("artist" + id);
        user.setNickname(nickname);
        user.setBio(nickname + "的个人简介");
        user.setTags(tags);
        user.setCertStatus(2);
        return user;
    }
}
