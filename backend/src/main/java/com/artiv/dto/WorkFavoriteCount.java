package com.artiv.dto;

import lombok.Data;

/** 作品收藏数批量聚合结果。 */
@Data
public class WorkFavoriteCount {
    private Long workId;
    private Long favoriteCount;
}
