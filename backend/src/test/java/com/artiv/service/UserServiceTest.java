package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.AccountProfileUpdateRequest;
import com.artiv.dto.PainterProfileUpdateRequest;
import com.artiv.dto.PasswordUpdateRequest;
import com.artiv.dto.RegisterRequest;
import com.artiv.dto.WorkbenchRoleUpdateRequest;
import com.artiv.entity.User;
import com.artiv.mapper.UserMapper;
import com.artiv.security.JwtUtil;
import com.artiv.service.impl.UserServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserMapper userMapper;
    @Mock JwtUtil jwtUtil;
    @Mock AccountRestrictionService accountRestrictionService;
    @InjectMocks UserServiceImpl userService;

    @Test
    void registerRejectsDuplicateNickname() {
        when(userMapper.selectCount(any())).thenReturn(0L, 1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.register(request("unique-account", "重复昵称")));

        assertEquals(ResultCode.NICKNAME_EXISTS.getCode(), exception.getCode());
        verify(userMapper, never()).insert(any(User.class));
    }

    @Test
    void registerTrimsNicknameBeforeCheckingAndSaving() {
        when(userMapper.selectCount(any())).thenReturn(0L, 0L);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        User registered = userService.register(request("unique-account", "  唯一昵称  "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).insert(captor.capture());
        assertEquals("唯一昵称", captor.getValue().getNickname());
        assertEquals("唯一昵称", registered.getNickname());
        assertNull(registered.getPassword());
    }

    @Test
    void concurrentNicknameConflictReturnsStableBusinessCode() {
        when(userMapper.selectCount(any())).thenReturn(0L, 0L, 1L);
        when(userMapper.insert(any(User.class)))
                .thenThrow(new DuplicateKeyException("uk_nickname"));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.register(request("race-account", "并发昵称")));

        assertEquals(ResultCode.NICKNAME_EXISTS.getCode(), exception.getCode());
    }

    @Test
    void updateAccountProfileRejectsAnotherUsersNickname() {
        when(userMapper.selectById(2L)).thenReturn(painter(2L, "原昵称"));
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateAccountProfile(2L, accountRequest("重复昵称")));

        assertEquals(ResultCode.NICKNAME_EXISTS.getCode(), exception.getCode());
        assertEquals("昵称已被使用，无法更改", exception.getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updateAccountProfileTrimsAndReturnsLatestUser() {
        User current = painter(2L, "原昵称");
        User updated = painter(2L, "新昵称");
        updated.setPassword("encoded-password");
        when(userMapper.selectById(2L)).thenReturn(current, updated);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        User result = userService.updateAccountProfile(2L, accountRequest("  新昵称  "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("新昵称", captor.getValue().getNickname());
        assertEquals("企划简介", captor.getValue().getEmployerBio());
        assertEquals("新昵称", result.getNickname());
        assertNull(result.getPassword());
    }

    @Test
    void updatePainterProfileKeepsIdentityFieldsSeparateForEmployerAccount() {
        User current = painter(2L, "原昵称");
        current.setRole(1);
        User updated = painter(2L, "原昵称");
        updated.setRole(1);
        updated.setPassword("encoded-password");
        when(userMapper.selectById(2L)).thenReturn(current, updated);

        PainterProfileUpdateRequest request = new PainterProfileUpdateRequest();
        request.setBio("  画师简介  ");
        request.setTags(java.util.List.of("日系", "场景"));
        User result = userService.updatePainterProfile(2L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("画师简介", captor.getValue().getBio());
        assertEquals("日系,场景", captor.getValue().getTags());
        assertNull(captor.getValue().getEmployerBio());
        assertNull(result.getPassword());
    }

    @Test
    void updatePasswordRejectsWrongCurrentPassword() {
        User current = painter(2L, "昵称");
        current.setPassword(new BCryptPasswordEncoder().encode("123456"));
        when(userMapper.selectById(2L)).thenReturn(current);
        PasswordUpdateRequest request = new PasswordUpdateRequest();
        request.setCurrentPassword("wrong-password");
        request.setNewPassword("654321");

        assertThrows(BusinessException.class, () -> userService.updatePassword(2L, request));
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void switchingWorkbenchPersistsRoleWithoutChangingPermissions() {
        User current = painter(2L, "昵称");
        current.setRole(1);
        User updated = painter(2L, "昵称");
        updated.setRole(2);
        updated.setPassword("encoded-password");
        when(userMapper.selectById(2L)).thenReturn(current, updated);

        WorkbenchRoleUpdateRequest request = new WorkbenchRoleUpdateRequest();
        request.setRole(2);
        User result = userService.updateWorkbenchRole(2L, request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals(2, captor.getValue().getRole());
        assertNull(captor.getValue().getCertStatus());
        assertEquals(2, result.getRole());
        assertNull(result.getPassword());
    }

    @Test
    void administratorCannotUseOrdinaryWorkbenchSwitch() {
        User admin = painter(3L, "管理员");
        admin.setRole(3);
        when(userMapper.selectById(3L)).thenReturn(admin);
        WorkbenchRoleUpdateRequest request = new WorkbenchRoleUpdateRequest();
        request.setRole(1);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updateWorkbenchRole(3L, request));

        assertEquals(ResultCode.NO_PERMISSION.getCode(), exception.getCode());
        verify(userMapper, never()).updateById(any(User.class));
    }

    private RegisterRequest request(String username, String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setNickname(nickname);
        request.setPassword("123456");
        request.setRole(2);
        return request;
    }

    private AccountProfileUpdateRequest accountRequest(String nickname) {
        AccountProfileUpdateRequest request = new AccountProfileUpdateRequest();
        request.setNickname(nickname);
        request.setEmployerBio("企划简介");
        return request;
    }

    private User painter(Long id, String nickname) {
        User user = new User();
        user.setId(id);
        user.setNickname(nickname);
        user.setRole(2);
        user.setDeleted(0);
        return user;
    }
}
