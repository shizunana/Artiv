package com.huiyue.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProjectRequirements {

    private String size;

    private String colorMode;

    private List<String> deliverables;

    private String notes;
}
