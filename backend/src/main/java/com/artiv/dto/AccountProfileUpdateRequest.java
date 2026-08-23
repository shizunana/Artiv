package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 企划方与画师共用的基础账户资料。 */
@Data
public class AccountProfileUpdateRequest {
    @NotBlank(message = "昵称不能为空")
    @Size(max = 50, message = "昵称不能超过50字")
    private String nickname;

    @Size(max = 500, message = "企划方个人简介不能超过500字")
    private String employerBio;
}
