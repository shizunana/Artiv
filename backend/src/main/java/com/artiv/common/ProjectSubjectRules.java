package com.artiv.common;

import java.util.*;

/** 企划专用题材：与作品、橱窗、画师的原 35 项隔离；空选择表示未限定。 */
public final class ProjectSubjectRules {
    public static final Map<String, List<String>> BY_CATEGORY = Map.of(
            "头像", List.of("Q版", "日系", "古风", "厚涂", "男性", "女性", "双人", "有背景", "黑白", "像素", "小动物", "安头"),
            "立绘", List.of("正比", "Q版", "日系", "古风", "厚涂", "男性", "女性", "有背景", "黑白", "定制"),
            "角色设计", List.of("正比", "Q版", "日系", "古风", "男性", "女性", "小动物"),
            "插画", List.of("日系", "古风", "厚涂", "场景", "氛围", "男性", "女性", "双人", "有背景", "横插", "竖插", "黑白", "像素", "小动物"),
            "平面设计", List.of("日系", "古风", "黑白", "像素", "Q版"),
            "自设/OC", List.of("正比", "Q版", "日系", "古风", "男性", "女性", "双人", "有背景", "小动物", "定制", "安头"));
    public static final List<String> OPTIONS = ProjectCategoryRules.OPTIONS.stream()
            .flatMap(category -> BY_CATEGORY.get(category).stream()).distinct().toList();
    private ProjectSubjectRules() { }

    public static List<String> normalize(String category, List<String> subjects) {
        var allowed = BY_CATEGORY.get(ProjectCategoryRules.require(category));
        if (subjects == null) return List.of();
        if (subjects.size() > OPTIONS.size()) throw invalid();
        var normalized = new LinkedHashSet<String>();
        for (String subject : subjects) {
            if (subject == null || !allowed.contains(subject.trim())) throw invalid();
            normalized.add(subject.trim());
        }
        return List.copyOf(normalized);
    }

    public static List<String> categories(List<String> categories) {
        if (categories == null || categories.isEmpty()) return List.of();
        if (categories.size() > 6) throw invalid();
        return categories.stream().map(ProjectCategoryRules::require).distinct().toList();
    }

    /** 无类型的旧单值调用可按企划题材全集查询，多类型不能夹带题材。 */
    public static String query(List<String> categories, String subject) {
        if (subject == null || subject.isBlank()) return null;
        String value = subject.trim();
        if (categories.size() > 1 || !OPTIONS.contains(value)) throw invalid();
        if (categories.size() == 1 && !BY_CATEGORY.get(categories.getFirst()).contains(value)) throw invalid();
        return value;
    }

    private static BusinessException invalid() {
        return new BusinessException(ResultCode.PARAM_INVALID.getCode(), "题材风格与企划类型不匹配；多选类型时请清空题材");
    }
}
