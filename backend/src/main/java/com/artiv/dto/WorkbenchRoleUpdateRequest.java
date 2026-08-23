package com.artiv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 普通账号默认工作台更新请求。role=1/2 只保存入口偏好，不授予业务权限。
 */
@Data
public class WorkbenchRoleUpdateRequest {

    @NotNull(message = "请选择工作台")
    @Min(value = 1, message = "工作台参数不合法")
    @Max(value = 2, message = "工作台参数不合法")
    private Integer role;
}
