package com.artiv.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 创建定向邀请入参；邀请状态和邀请人身份由服务端确定。 */
@Data
public class InvitationRequest {

    @NotNull(message = "企划不能为空")
    private Long projectId;

    @NotNull(message = "受邀画师不能为空")
    private Long painterId;

    @Size(max = 500, message = "邀请说明最长 500 个字")
    private String message;
}
