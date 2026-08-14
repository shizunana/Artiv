package com.artiv.service;

import com.artiv.dto.LoginRequest;
import com.artiv.dto.RegisterRequest;
import com.artiv.entity.User;

import java.util.Map;

public interface UserService {

    User register(RegisterRequest request);

    Map<String, Object> login(LoginRequest request);

    User getById(Long id);
}
