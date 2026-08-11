package com.huiyue.controller;

import com.huiyue.common.Result;
import com.huiyue.dto.LoginRequest;
import com.huiyue.dto.RegisterRequest;
import com.huiyue.entity.User;
import com.huiyue.security.UserContext;
import com.huiyue.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<User> register(@Valid @RequestBody RegisterRequest request) {
        return Result.ok(userService.register(request));
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginRequest request) {
        return Result.ok(userService.login(request));
    }

    @GetMapping("/me")
    public Result<User> me() {
        return Result.ok(userService.getById(UserContext.getUserId()));
    }
}
