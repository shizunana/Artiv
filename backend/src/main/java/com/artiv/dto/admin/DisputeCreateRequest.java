package com.artiv.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DisputeCreateRequest {
    @NotBlank(message = "争议原因不能为空")
    @Size(max = 500)
    private String reason;

    @Size(max = 9, message = "证据图片最多9张")
    private List<String> images;

    @Size(max = 500)
    private String description;
}
