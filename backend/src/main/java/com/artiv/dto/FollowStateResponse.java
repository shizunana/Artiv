package com.artiv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FollowStateResponse {
    private Long painterId;
    private boolean followed;
}
