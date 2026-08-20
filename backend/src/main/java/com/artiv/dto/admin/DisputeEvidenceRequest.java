package com.artiv.dto.admin;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class DisputeEvidenceRequest {
    @NotEmpty(message = "请至少上传一张证据图片")
    @Size(max = 9, message = "证据图片最多9张")
    private List<String> images;

    @Size(max = 500)
    private String description;
}
