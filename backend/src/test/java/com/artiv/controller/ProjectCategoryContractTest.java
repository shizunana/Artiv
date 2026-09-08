package com.artiv.controller;

import com.artiv.common.GlobalExceptionHandler;
import com.artiv.controller.admin.AdminStatsController;
import com.artiv.entity.Project;
import com.artiv.service.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class ProjectCategoryContractTest {
    ProjectService service = mock(ProjectService.class);
    AdminStatsService stats = mock(AdminStatsService.class);
    MockMvc mvc = MockMvcBuilders.standaloneSetup(new ProjectController(service), new AdminStatsController(stats))
            .setControllerAdvice(new GlobalExceptionHandler()).build();

    @Test void oldAndDoubleQueryParametersAreExplicitlyRejected() throws Exception {
        for (String path : new String[]{"/api/v1/projects", "/api/v1/admin/stats/price-distribution"}) {
            mvc.perform(get(path).param("category", "头像")).andExpect(jsonPath("code").value(2001));
            mvc.perform(get(path).param("category", "").param("projectCategory", "插画")).andExpect(jsonPath("code").value(2001));
        }
        verifyNoInteractions(service, stats);
        mvc.perform(get("/api/v1/admin/stats/price-distribution").param("projectCategory", "插画"));
        verify(stats).priceDistribution(null, null, "插画");
        mvc.perform(get("/api/v1/projects").param("projectCategory", "自设/OC"));
        verify(service).listProjects(1,10,null,null,null,null,null,null,null,null,"自设/OC",null);
    }

    @Test void multiCategoryBindingAndInvalidCombinations() throws Exception {
        mvc.perform(get("/api/v1/projects").param("projectCategories", "头像", "插画", "头像"));
        verify(service).listProjectsByCategories(1,10,null,null,null,null,null,null,null,null,java.util.List.of("头像","插画"),null);
        clearInvocations(service);
        for (var request : java.util.List.of(
                get("/api/v1/projects").param("projectCategories","头像").param("projectCategory",""),
                get("/api/v1/projects").param("projectCategories","头像","插画").param("subject","古风"),
                get("/api/v1/projects").param("projectCategories","Live2d"),
                get("/api/v1/projects").param("projectCategories","头像").param("subject","场景"))) {
            mvc.perform(request).andExpect(jsonPath("code").value(2001));
        }
        verifyNoInteractions(service);
    }

    @Test void multiSubjectBindingUsesUnionQueryContract() throws Exception {
        mvc.perform(get("/api/v1/projects").param("projectCategories", "头像")
                .param("subjects", "古风", "日系", "古风"));
        verify(service).listProjectsByCategoriesAndSubjects(1,10,null,null,null,null,null,null,null,null,
                java.util.List.of("头像"),java.util.List.of("古风","日系"));
        clearInvocations(service);
        for (var request : java.util.List.of(
                get("/api/v1/projects").param("projectCategories","头像","插画").param("subjects","古风"),
                get("/api/v1/projects").param("projectCategories","头像").param("subjects","场景"),
                get("/api/v1/projects").param("projectCategories","头像").param("subject","古风").param("subjects","日系"))) {
            mvc.perform(request).andExpect(jsonPath("code").value(2001));
        }
        verifyNoInteractions(service);
    }

    @Test void oldJsonFieldIncludingNullCannotBeIgnored() throws Exception {
        for (String legacy : new String[]{"null", "\"头像\""}) {
            String body = "{\"title\":\"测试\",\"description\":\"需求\",\"deadline\":\"2026-12-01\",\"projectCategory\":\"插画\",\"category\":"+legacy+"}";
            mvc.perform(post("/api/v1/projects").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(jsonPath("code").value(2001)).andExpect(jsonPath("message").value(org.hamcrest.Matchers.containsString("category")));
            mvc.perform(put("/api/v1/projects/1/content").contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(jsonPath("code").value(2001));
        }
        verifyNoInteractions(service);
        Project p = new Project(); p.setProjectCategory("角色设计");
        var json = new ObjectMapper().valueToTree(p);
        assertEquals("角色设计", json.path("projectCategory").asText());
        assertFalse(json.has("category"));
    }
}
