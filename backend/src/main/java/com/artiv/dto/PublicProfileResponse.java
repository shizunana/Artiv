package com.artiv.dto;

import lombok.Data;

import java.util.List;

/**
 * 用户公开主页资料。
 *
 * <p>该 DTO 只包含公开展示字段，禁止加入用户名、余额、手机号等账户私密信息。</p>
 */
@Data
public class PublicProfileResponse {

    private Long id;

    private String nickname;

    private String avatar;

    private String bio;

    private List<String> tags;

    private Integer certStatus;

    private boolean painterProfileAvailable;

    private boolean employerProfileAvailable;

    /** 画师已结束订单中的完成比例；企划方主页及没有样本时为 null。 */
    private Integer completionRate;

    private long completionCompletedCount;

    private long completionTerminalCount;

    /** 有明确企划截稿日期的已完成订单中，按时完成的比例；没有样本时为 null。 */
    private Integer onTimeRate;

    private long onTimeCompletedCount;

    private long onTimeEligibleCount;

    private long publicWorkCount;

    private long publicStallCount;

    private long publicProjectCount;

    /** 当前主页身份收到的可见评价平均分；没有评价时为 null。 */
    private Double averageScore;

    private long reviewCount;
}
