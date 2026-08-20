package com.artiv.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 业务错误码
 */
@Getter
@AllArgsConstructor
public enum ResultCode {

    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未登录或登录过期"),
    FORBIDDEN(403, "无权操作该资源"),
    NOT_FOUND(404, "资源不存在"),
    ERROR(500, "服务器内部错误"),

    USERNAME_EXISTS(1001, "用户名已存在"),
    LOGIN_FAILED(1002, "用户名或密码错误"),
    TOKEN_INVALID(1003, "登录过期，请重新登录"),
    NICKNAME_EXISTS(1004, "昵称已存在"),
    PARAM_INVALID(2001, "参数校验失败"),
    RESOURCE_NOT_FOUND(2002, "资源不存在"),
    NO_PERMISSION(2003, "无权操作该资源"),
    STATUS_FLOW_INVALID(3001, "状态流转不合法"),
    BUDGET_NOT_ENOUGH(3002, "预算不足"),
    UPLOAD_INVALID(4001, "上传文件类型或大小不合法");

    private final int code;
    private final String message;
}
