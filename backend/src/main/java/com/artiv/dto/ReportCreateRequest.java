package com.artiv.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class ReportCreateRequest {
    @NotNull
    private Long targetUserId;

    @NotBlank
    @Size(max = 30)
    private String category;

    @NotBlank
    @Size(min = 10, max = 500)
    private String reason;

    @Size(max = 6)
    private List<String> images;
}
