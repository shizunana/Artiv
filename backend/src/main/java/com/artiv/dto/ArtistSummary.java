package com.artiv.dto;

import com.artiv.entity.Work;
import lombok.Data;

import java.util.List;

/**
 * 画师大厅列表项：画师公开资料、可筛选类型与公开作品预览。
 */
@Data
public class ArtistSummary {

    private Long id;

    private String username;

    private String nickname;

    private String avatar;

    private String bio;

    private String tags;

    private Integer certStatus;

    private List<String> categories;

    private List<Work> works;

    private Double averageScore;

    private long reviewCount;
}
