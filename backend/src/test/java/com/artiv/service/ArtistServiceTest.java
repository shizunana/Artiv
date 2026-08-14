package com.artiv.service;

import com.artiv.dto.ArtistSummary;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.ArtistServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ArtistServiceTest {

    @Mock
    private UserMapper userMapper;

    @Mock
    private WorkMapper workMapper;

    @InjectMocks
    private ArtistServiceImpl artistService;

    @Test
    void certifiedPaintersRemainVisibleWithoutWorks() {
        when(userMapper.selectList(any())).thenReturn(List.of(
                painter(14L, "测试画师1", "日系,立绘,女性"),
                painter(15L, "测试画师2", "Q版,场景,可爱")
        ));
        when(workMapper.selectList(any())).thenReturn(List.of());

        List<ArtistSummary> artists = artistService.listArtists(null, null, null);

        assertEquals(2, artists.size());
        assertEquals("测试画师1", artists.get(0).getNickname());
        assertEquals(List.of(), artists.get(0).getWorks());
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

        List<ArtistSummary> artists = artistService.listArtists("立绘", "日系,女性", "测试画师");

        assertEquals(1, artists.size());
        assertEquals(14L, artists.get(0).getId());
        assertEquals(List.of("立绘"), artists.get(0).getCategories());
        assertEquals(1, artists.get(0).getWorks().size());
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
