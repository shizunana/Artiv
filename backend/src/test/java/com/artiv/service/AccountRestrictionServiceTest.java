package com.artiv.service;

import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import com.artiv.service.impl.AccountRestrictionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountRestrictionServiceTest {

    @Mock private UserMapper userMapper;
    @Mock private AdminAuditService auditService;
    @InjectMocks private AccountRestrictionServiceImpl accountRestrictionService;

    @Test
    void expiredBanClearsAllMetadataAndWritesAuditOnce() {
        User user = expiredUser();
        when(userMapper.selectById(8L)).thenReturn(user);
        when(userMapper.clearExpiredBan(eq(8L), any(LocalDateTime.class))).thenReturn(1);

        User refreshed = accountRestrictionService.refresh(8L);

        assertEquals(0, refreshed.getStatus());
        assertNull(refreshed.getBanReason());
        assertNull(refreshed.getBannedBy());
        assertNull(refreshed.getBannedAt());
        assertNull(refreshed.getBannedUntil());
        verify(userMapper).clearExpiredBan(eq(8L), any(LocalDateTime.class));
        verify(userMapper, never()).updateById(any(User.class));
        verify(auditService).record(eq(null), eq("AUTO_UNBAN_USER"), eq("USER"),
                eq(8L), any(), eq(user), eq("限时封禁到期自动解封"));
    }

    private User expiredUser() {
        User user = new User();
        user.setId(8L);
        user.setStatus(1);
        user.setDeleted(0);
        user.setBanReason("限时封禁");
        user.setBannedBy(3L);
        user.setBannedAt(LocalDateTime.now().minusHours(1));
        user.setBannedUntil(LocalDateTime.now().minusSeconds(1));
        return user;
    }
}
