package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import com.artiv.dto.PainterProfileUpdateRequest;
import com.artiv.dto.RegisterRequest;
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
    void updatePainterProfileRejectsAnotherUsersNickname() {
        when(userMapper.selectById(2L)).thenReturn(painter(2L, "原昵称"));
        when(userMapper.selectCount(any())).thenReturn(1L);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> userService.updatePainterProfile(2L, profileRequest("重复昵称")));

        assertEquals(ResultCode.NICKNAME_EXISTS.getCode(), exception.getCode());
        assertEquals("昵称已被使用，无法更改", exception.getMessage());
        verify(userMapper, never()).updateById(any(User.class));
    }

    @Test
    void updatePainterProfileTrimsAndReturnsLatestUser() {
        User current = painter(2L, "原昵称");
        User updated = painter(2L, "新昵称");
        updated.setPassword("encoded-password");
        when(userMapper.selectById(2L)).thenReturn(current, updated);
        when(userMapper.selectCount(any())).thenReturn(0L);
        when(userMapper.updateById(any(User.class))).thenReturn(1);

        User result = userService.updatePainterProfile(2L, profileRequest("  新昵称  "));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userMapper).updateById(captor.capture());
        assertEquals("新昵称", captor.getValue().getNickname());
        assertEquals("新昵称", result.getNickname());
        assertNull(result.getPassword());
    }

    private RegisterRequest request(String username, String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setNickname(nickname);
        request.setPassword("123456");
        request.setRole(2);
        return request;
    }

    private PainterProfileUpdateRequest profileRequest(String nickname) {
        PainterProfileUpdateRequest request = new PainterProfileUpdateRequest();
        request.setNickname(nickname);
        request.setBio("简介");
        request.setTags(java.util.List.of("日系"));
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
