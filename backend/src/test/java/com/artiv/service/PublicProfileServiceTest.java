package com.artiv.service;

import com.artiv.dto.PublicProfileResponse;
import com.artiv.entity.User;
import com.artiv.mapper.OrderInfoMapper;
import com.artiv.mapper.ProjectMapper;
import com.artiv.mapper.StallMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.impl.PublicProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PublicProfileServiceTest {

    @Mock UserMapper userMapper;
    @Mock WorkMapper workMapper;
    @Mock StallMapper stallMapper;
    @Mock ProjectMapper projectMapper;
    @Mock OrderInfoMapper orderInfoMapper;
    @InjectMocks PublicProfileServiceImpl publicProfileService;

    @Test
    void unverifiedUserCanOpenPainterProfileAndSeePublicWorkCount() {
        User user = activeUser(21L, 0);
        when(userMapper.selectById(21L)).thenReturn(user);
        when(workMapper.selectCount(any())).thenReturn(2L);
        when(stallMapper.selectCount(any())).thenReturn(0L);
        when(projectMapper.selectCount(any())).thenReturn(1L);
        when(orderInfoMapper.selectList(any())).thenReturn(List.of());

        PublicProfileResponse profile = publicProfileService.getProfile(21L, "painter");

        assertTrue(profile.isPainterProfileAvailable());
        assertEquals(0, profile.getCertStatus());
        assertEquals(2, profile.getPublicWorkCount());
        assertNull(profile.getCompletionRate());
    }

    @Test
    void employerProfileDoesNotCalculateOrExposeDemandCompletionRate() {
        User user = activeUser(22L, 2);
        when(userMapper.selectById(22L)).thenReturn(user);
        when(workMapper.selectCount(any())).thenReturn(0L);
        when(stallMapper.selectCount(any())).thenReturn(0L);
        when(projectMapper.selectCount(any())).thenReturn(3L);

        PublicProfileResponse profile = publicProfileService.getProfile(22L, "employer");

        assertTrue(profile.isEmployerProfileAvailable());
        assertNull(profile.getCompletionRate());
        assertEquals(0, profile.getCompletionTerminalCount());
        verify(orderInfoMapper, never()).selectList(any());
    }

    private User activeUser(Long id, int certStatus) {
        User user = new User();
        user.setId(id);
        user.setNickname("测试用户" + id);
        user.setStatus(0);
        user.setDeleted(0);
        user.setCertStatus(certStatus);
        return user;
    }
}
