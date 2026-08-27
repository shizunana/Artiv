package com.artiv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FavoriteStateResponse {
    private Long targetId;
    private boolean favorited;
    private Long favoriteCount;
}
