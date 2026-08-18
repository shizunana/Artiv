package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 当前用户头像更新请求。 */
@Data
public class AvatarUpdateRequest {

    @NotBlank(message = "头像地址不能为空")
    @Size(max = 255, message = "头像地址最长 255 个字符")
    private String avatar;
}
