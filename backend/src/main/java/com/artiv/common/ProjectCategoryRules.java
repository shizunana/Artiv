package com.artiv.common;

import java.util.List;
import java.util.Map;

/** 企划专用六类；供给映射仅用于统计，不用于发布或大厅并集查询。 */
public final class ProjectCategoryRules {
    public static final List<String> OPTIONS = List.of("头像", "立绘", "角色设计", "插画", "平面设计", "自设/OC");
    private static final Map<String, List<String>> SUPPLY = Map.of(
            "头像", List.of("头像", "表情包"),
            "立绘", List.of("立绘", "Q版全身"),
            "角色设计", List.of("服设", "组合页", "立绘", "Live2d"),
            "插画", List.of("半身像", "壁纸"),
            "平面设计", List.of("平面设计", "表情包"),
            "自设/OC", List.of("Q版全身", "半身像", "立绘", "组合页", "服设"));

    private ProjectCategoryRules() { }

    public static String require(String value) {
        if (value == null || !OPTIONS.contains(value.trim())) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "请选择有效的企划类型：" + String.join("、", OPTIONS));
        }
        return value.trim();
    }

    public static String optional(String value) {
        return value == null || value.isBlank() ? null : require(value);
    }

    public static List<String> supplyCategories(String projectCategory) {
        return SUPPLY.get(require(projectCategory));
    }
}
