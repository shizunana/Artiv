package com.artiv.service;

import com.artiv.dto.PublicProfileResponse;
import com.artiv.dto.ReviewStatistics;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.Project;
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
import java.time.LocalDate;
import java.time.LocalDateTime;

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
    @Mock ReviewService reviewService;
    @InjectMocks PublicProfileServiceImpl publicProfileService;

    @Test
    void unverifiedUserCanOpenPainterProfileAndSeePublicWorkCount() {
        User user = activeUser(21L, 0);
        when(userMapper.selectById(21L)).thenReturn(user);
        when(workMapper.selectCount(any())).thenReturn(2L);
        when(stallMapper.selectCount(any())).thenReturn(0L);
        when(projectMapper.selectCount(any())).thenReturn(1L);
        when(orderInfoMapper.selectList(any())).thenReturn(List.of());
        when(reviewService.getStatistics(21L, "painter")).thenReturn(new ReviewStatistics(null, 0));

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
        when(reviewService.getStatistics(22L, "employer")).thenReturn(new ReviewStatistics(null, 0));

        PublicProfileResponse profile = publicProfileService.getProfile(22L, "employer");

        assertTrue(profile.isEmployerProfileAvailable());
        assertNull(profile.getCompletionRate());
        assertEquals(0, profile.getCompletionTerminalCount());
        verify(orderInfoMapper, never()).selectList(any());
    }

    @Test
    void painterOnTimeRateUsesOrderDeadlineSnapshotInsteadOfLaterProjectEdits() {
        User user = activeUser(21L, 2);
        when(userMapper.selectById(21L)).thenReturn(user);
        when(workMapper.selectCount(any())).thenReturn(0L);
        when(stallMapper.selectCount(any())).thenReturn(0L);
        when(projectMapper.selectCount(any())).thenReturn(1L);
        when(reviewService.getStatistics(21L, "painter")).thenReturn(new ReviewStatistics(null, 0));

        OrderInfo completed = new OrderInfo();
        completed.setId(44L);
        completed.setProjectId(8L);
        completed.setPainterId(21L);
        completed.setStatus(3);
        completed.setCompletedAt(LocalDateTime.of(2026, 8, 20, 12, 0));
        completed.setProjectSnapshot("{\"deadline\":\"2026-08-19\"}");
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(completed));

        Project editedLater = new Project();
        editedLater.setId(8L);
        editedLater.setDeadline(LocalDate.of(2026, 8, 30));
        when(projectMapper.selectBatchIds(List.of(8L))).thenReturn(List.of(editedLater));

        PublicProfileResponse profile = publicProfileService.getProfile(21L, "painter");

        assertEquals(0, profile.getOnTimeRate());
        assertEquals(0, profile.getOnTimeCompletedCount());
        assertEquals(1, profile.getOnTimeEligibleCount());
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
