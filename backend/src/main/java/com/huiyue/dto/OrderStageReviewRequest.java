package com.huiyue.dto;

import lombok.Data;

@Data
public class OrderStageReviewRequest {
    /** approve（进入下一阶段）、complete（直接完成交易）或 revise（要求修改） */
    private String action;
    private String remark;
}
