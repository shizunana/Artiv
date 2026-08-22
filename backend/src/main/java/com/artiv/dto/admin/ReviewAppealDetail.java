package com.artiv.dto.admin;

import com.artiv.dto.ReviewListItem;
import com.artiv.entity.ReviewAppeal;
import lombok.Data;

@Data
public class ReviewAppealDetail {
    private ReviewAppeal appeal;
    private ReviewListItem review;
    private Long applicantId;
    private String applicantNickname;
    private String applicantAvatar;
}
