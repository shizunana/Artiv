package com.artiv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BlockStatusResponse {
    private Long userId;
    private boolean blocked;
}
