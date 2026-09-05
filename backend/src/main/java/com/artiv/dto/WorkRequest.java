package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/** 上传作品入参。图片由上传接口先返回地址后再提交。 */
@Data
public class WorkRequest {

    @NotBlank(message = "作品标题不能为空")
    @Size(max = 20, message = "作品标题最长 20 个字")
    private String title;

    @NotBlank(message = "作品类型不能为空")
    @Size(max = 50, message = "作品类型最长 50 个字")
    private String category;

    @NotBlank(message = "请选择至少一个题材风格")
    @Size(max = 500, message = "题材风格内容过长")
    private String subject;

    @NotBlank(message = "请上传作品图片")
    @Size(max = 255, message = "作品图片地址过长")
    private String image;

    @Size(max = 500, message = "作品说明最长 500 个字")
    private String description;

    /** 0 仅自己可见、1 公开 */
    @NotNull(message = "请选择作品可见范围")
    private Integer visibility;
}
