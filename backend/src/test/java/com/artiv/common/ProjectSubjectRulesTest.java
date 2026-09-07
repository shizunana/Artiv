package com.artiv.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectSubjectRulesTest {
    @Test void sixMappingsMatchGuideAndKeepOriginalSubjects() throws Exception {
        try (var in = getClass().getResourceAsStream("/guide/search-options.json")) {
            var json = new ObjectMapper(); var options = json.readTree(in);
            assertEquals(json.valueToTree(ProjectSubjectRules.BY_CATEGORY), options.path("projectSubjectsByCategory"));
            assertEquals(json.valueToTree(ProjectSubjectRules.OPTIONS), options.path("projectSubjects"));
            assertEquals(35, options.path("subjects").size());
            assertEquals(18, ProjectSubjectRules.OPTIONS.size());
        }
    }
    @Test void normalizeRejectsWrongGroupAndAllowsEmpty() {
        for (String category : ProjectCategoryRules.OPTIONS) {
            assertEquals(List.of(), ProjectSubjectRules.normalize(category, null));
            assertEquals(ProjectSubjectRules.BY_CATEGORY.get(category), ProjectSubjectRules.normalize(category, ProjectSubjectRules.BY_CATEGORY.get(category)));
        }
        assertEquals(List.of("古风"), ProjectSubjectRules.normalize("头像", List.of("古风", " 古风 ")));
        for (String invalid : List.of("动图", "不限", "LOGO", "场景", "古风,日系", "")) {
            assertThrows(BusinessException.class, () -> ProjectSubjectRules.normalize("头像", List.of(invalid)));
        }
        assertThrows(BusinessException.class, () -> ProjectSubjectRules.normalize("头像", Arrays.asList((String)null)));
    }
    @Test void queryModesAreExplicit() {
        assertEquals(List.of("头像","插画"), ProjectSubjectRules.categories(List.of("头像","插画","头像")));
        assertThrows(BusinessException.class, () -> ProjectSubjectRules.categories(Collections.nCopies(7,"头像")));
        assertThrows(BusinessException.class, () -> ProjectSubjectRules.categories(List.of("Live2d")));
        assertNull(ProjectSubjectRules.query(List.of("头像","插画"), ""));
        assertEquals("古风", ProjectSubjectRules.query(List.of(),"古风"));
        assertThrows(BusinessException.class, () -> ProjectSubjectRules.query(List.of("头像","插画"),"古风"));
        assertThrows(BusinessException.class, () -> ProjectSubjectRules.query(List.of("角色设计"),"像素"));
        assertThrows(BusinessException.class, () -> ProjectSubjectRules.query(List.of(),"%"));
    }
}
