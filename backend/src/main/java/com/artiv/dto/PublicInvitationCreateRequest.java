package com.artiv.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PublicInvitationCreateRequest {

    @NotNull(message = "请选择公开企划")
    private Long projectId;

    @NotNull(message = "请选择画师")
    private Long painterId;

    /** 1 邀请报价、2 带价邀请 */
    @NotNull(message = "请选择邀请类型")
    private Integer invitationType;

    @DecimalMin(value = "50.00", message = "邀请价格不能低于 50 元")
    private BigDecimal offeredPrice;

    @Size(max = 500, message = "附言不能超过 500 字")
    private String message;
}
