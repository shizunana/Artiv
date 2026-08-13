package com.huiyue.dto;

import lombok.Data;

import java.util.List;

@Data
public class OrderStageSubmitRequest {
    private String content;
    private List<String> previewImages;
    private List<String> originalFiles;
}
