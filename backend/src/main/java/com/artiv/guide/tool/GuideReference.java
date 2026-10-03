package com.artiv.guide.tool;

import java.util.List;

/** 引用只能由后端已执行的白名单读取生成；不接受模型生成的 URL 或源码路径。 */
public record GuideReference(String kind, String id, String title, String detail, String checkedAt, Card card) {
    // 规则、查询元信息及旧引用保留原协议，不生成推荐卡片。
    public GuideReference(String kind, String id, String title, String detail, String checkedAt) {
        this(kind, id, title, detail, checkedAt, null);
    }
    /** 仅由公开资源的白名单投影生成；不包含原始查询信息或模型提供的链接。 */
    public record Card(String category, List<Field> fields, String summary,
                       String coverUrl, String avatarUrl, String creatorName) {
        public Card(String category, List<Field> fields, String summary) {
            this(category, fields, summary, null, null, null);
        }
    }
    public record Field(String label, String value) { }
}
