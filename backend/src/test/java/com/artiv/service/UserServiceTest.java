package com.artiv.service;

import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
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

    private RegisterRequest request(String username, String nickname) {
        RegisterRequest request = new RegisterRequest();
        request.setUsername(username);
        request.setNickname(nickname);
        request.setPassword("123456");
        request.setRole(2);
        return request;
    }
}
