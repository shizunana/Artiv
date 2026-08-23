package com.artiv.service;

import com.artiv.dto.LoginRequest;
import com.artiv.dto.AccountProfileUpdateRequest;
import com.artiv.dto.PasswordUpdateRequest;
import com.artiv.dto.PainterProfileUpdateRequest;
import com.artiv.dto.RegisterRequest;
import com.artiv.entity.User;

import java.util.Map;

public interface UserService {

    User register(RegisterRequest request);

    User updatePainterProfile(Long userId, PainterProfileUpdateRequest request);

    User updateAccountProfile(Long userId, AccountProfileUpdateRequest request);

    void updatePassword(Long userId, PasswordUpdateRequest request);

    Map<String, Object> login(LoginRequest request);

    User getById(Long id);
}
