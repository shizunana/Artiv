package com.artiv.dto;

import lombok.Data;

/**
 * 当前页画师的公开作品类型聚合结果。
 */
@Data
public class ArtistCategoryAggregate {

    private Long userId;

    /** 多个类型使用英文逗号分隔。 */
    private String categories;
}
