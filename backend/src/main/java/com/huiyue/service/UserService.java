package com.huiyue.service;

import com.huiyue.dto.LoginRequest;
import com.huiyue.dto.RegisterRequest;
import com.huiyue.entity.User;

import java.util.Map;

public interface UserService {

    User register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    User getById(Long id);
}
