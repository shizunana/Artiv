package com.artiv.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectCategoryRulesTest {
    @Test void sixIndependentValuesAndSupplyCoverage() throws Exception {
        assertEquals(List.of("头像","立绘","角色设计","插画","平面设计","自设/OC"), ProjectCategoryRules.OPTIONS);
        for (String value : ProjectCategoryRules.OPTIONS) assertEquals(value, ProjectCategoryRules.require(value));
        for (String invalid : List.of("", "全部", "其他", "服设", "Live2d", "头像,立绘")) {
            assertThrows(BusinessException.class, () -> ProjectCategoryRules.require(invalid));
        }
        assertThrows(BusinessException.class, () -> ProjectCategoryRules.require(null));
        assertNull(ProjectCategoryRules.optional(" "));
        Set<String> supply = new HashSet<>();
        for (String value : ProjectCategoryRules.OPTIONS) supply.addAll(ProjectCategoryRules.supplyCategories(value));
        assertEquals(Set.of("头像","Q版全身","半身像","立绘","组合页","服设","壁纸","表情包","Live2d","平面设计"), supply);
        assertFalse(ProjectCategoryRules.supplyCategories("自设/OC").contains("Live2d"));
        assertFalse(ProjectCategoryRules.supplyCategories("自设/OC").contains("头像"));
        assertFalse(ProjectCategoryRules.supplyCategories("插画").contains("立绘"));
        try (var stream = getClass().getResourceAsStream("/guide/search-options.json")) {
            var options = new ObjectMapper().readTree(stream);
            assertEquals(ProjectCategoryRules.OPTIONS, new ObjectMapper().convertValue(options.get("projectCategories"), List.class));
            assertEquals(supply, new HashSet<>(new ObjectMapper().convertValue(options.get("categories"), List.class)));
        }
    }
}
