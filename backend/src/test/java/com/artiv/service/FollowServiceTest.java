package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.entity.User;
import com.artiv.entity.UserFollow;
import com.artiv.mapper.UserFollowMapper;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.FollowServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FollowServiceTest {

    @Mock private UserFollowMapper userFollowMapper;
    @Mock private UserMapper userMapper;
    @Mock private AccountRestrictionService accountRestrictionService;

    @InjectMocks private FollowServiceImpl followService;

    @Test
    void followUsesCertificationInsteadOfRegistrationRole() {
        when(userMapper.selectById(16L)).thenReturn(painter(16L, 1, 2));
        when(userFollowMapper.selectCount(any())).thenReturn(0L);

        var state = followService.followPainter(16L, 3L);

        assertTrue(state.isFollowed());
        ArgumentCaptor<UserFollow> captor = ArgumentCaptor.forClass(UserFollow.class);
        verify(userFollowMapper).insert(captor.capture());
        assertEquals(3L, captor.getValue().getUserId());
        assertEquals(16L, captor.getValue().getPainterId());
    }

    @Test
    void followRejectsUncertifiedTargetEvenWhenRoleIsPainter() {
        when(userMapper.selectById(16L)).thenReturn(painter(16L, 2, 0));

        assertThrows(BusinessException.class, () -> followService.followPainter(16L, 3L));
        verify(userFollowMapper, never()).insert(any(UserFollow.class));
    }

    @Test
    void followRejectsSelf() {
        when(userMapper.selectById(16L)).thenReturn(painter(16L, 1, 2));

        assertThrows(BusinessException.class, () -> followService.followPainter(16L, 16L));
        verify(userFollowMapper, never()).insert(any(UserFollow.class));
    }

    @Test
    void unfollowDoesNotRequireAccountAvailability() {
        var state = followService.unfollowPainter(16L, 3L);

        assertFalse(state.isFollowed());
        verify(userFollowMapper).delete(any());
        verify(accountRestrictionService, never()).requireAvailable(any(), any());
    }

    private User painter(Long id, int role, int certStatus) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        user.setCertStatus(certStatus);
        user.setStatus(0);
        user.setDeleted(0);
        return user;
    }
}
