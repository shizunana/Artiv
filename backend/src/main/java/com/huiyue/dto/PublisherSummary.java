package com.huiyue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PublisherSummary {

    private Long id;

    private String nickname;

    private String avatar;

    private String bio;

    private String tags;
}
