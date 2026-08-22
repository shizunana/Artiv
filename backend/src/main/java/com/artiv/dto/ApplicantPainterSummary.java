package com.artiv.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

/** 企划应征列表使用的画师公开资料，避免公开余额、账号状态等内部字段。 */
@Data
@NoArgsConstructor
public class ApplicantPainterSummary {

    private Long id;
    private String nickname;
    private String avatar;
    private String bio;
    private String tags;
    private Double averageScore;
    private long reviewCount;
}
