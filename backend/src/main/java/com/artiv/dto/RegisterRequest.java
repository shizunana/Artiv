package com.artiv.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度需在 3-20 位之间")
    private String username;

    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 32, message = "密码长度需在 6-32 位之间")
    private String password;

    @NotBlank(message = "昵称不能为空")
    private String nickname;

    /** 注册主身份：1 默认企划方工作台、2 默认画师工作台；不代表永久权限。 */
    @Min(value = 1, message = "角色不合法")
    @Max(value = 2, message = "角色不合法")
    private Integer role;
}
