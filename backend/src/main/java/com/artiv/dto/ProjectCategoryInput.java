package com.artiv.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.AssertFalse;

/** 显式拒绝旧客户端字段，包括 null 和新旧双传；不影响其他模块的 category。 */
public abstract class ProjectCategoryInput {
    private boolean legacyCategoryPresent;

    @JsonSetter("category")
    public void rejectLegacyCategory(JsonNode ignored) {
        legacyCategoryPresent = true;
    }

    @JsonIgnore
    @AssertFalse(message = "企划字段 category 已停用，请刷新页面后使用 projectCategory")
    public boolean isLegacyCategoryPresent() {
        return legacyCategoryPresent;
    }
}
