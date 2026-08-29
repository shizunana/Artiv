package com.artiv.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 企划面板参与画师分组所需的最小公开资料。 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderParticipantSummary {
    private Long id;
    private String nickname;
    private String avatar;
}
