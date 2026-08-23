package com.artiv.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/** 当前用户可直接维护、且不进入认证审核的画师展示资料。 */
@Data
public class PainterProfileUpdateRequest {

    @Size(max = 500, message = "个人简介不能超过500字")
    private String bio;

    /** 仅接收前端共享题材风格选项中的值，持久化时以英文逗号分隔。 */
    @Size(max = 35, message = "风格标签数量不合法")
    private List<String> tags;
}
