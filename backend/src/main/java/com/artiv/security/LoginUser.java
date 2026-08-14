package com.artiv.security;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 当前登录用户上下文（由拦截器填充到 ThreadLocal）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginUser {

    private Long id;
    private String username;
    private Integer role;
}
