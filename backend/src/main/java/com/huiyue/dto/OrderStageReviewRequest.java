package com.huiyue.dto;

import lombok.Data;

@Data
public class OrderStageReviewRequest {
    /** approve 或 revise */
    private String action;
    private String remark;
}
