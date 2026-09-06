package com.artiv.guide.tool;

import com.artiv.common.BusinessException;
import com.artiv.dto.ArtistSummary;
import com.artiv.entity.*;
import com.artiv.mapper.*;
import com.artiv.service.*;
import com.artiv.guide.client.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class GuidePublicReaderTest {
    ProjectService projects = mock(ProjectService.class); ArtistService artists = mock(ArtistService.class);
    ProjectMapper projectMapper = mock(ProjectMapper.class); UserMapper users = mock(UserMapper.class); ReviewMapper reviews = mock(ReviewMapper.class);
    GuidePublicReader reader = new GuidePublicReader(projects, artists, projectMapper, users, reviews);
    ObjectMapper json = new ObjectMapper();
    Project project() {
        var p = new Project(); p.setId(11L); p.setMode(0); p.setStatus(0); p.setDeleted(0); p.setModerationStatus(0);
        p.setDeadline(LocalDate.now()); p.setTitle("公开测试企划"); p.setEmployerId(991L); p.setImages("private-image-marker"); p.setModerationReason("private-moderation-marker"); return p;
    }
    User artist() {
        var u = new User(); u.setId(21L); u.setStatus(0); u.setDeleted(0); u.setCertStatus(2); u.setPassword("secret-hash-marker"); return u;
    }
    @Test void projectProjectionContainsOnlyPublicFieldsAndNeverUsesAdminDetail() throws Exception {
        when(projectMapper.selectById(11L)).thenReturn(project());
        var result = reader.project(11);
        String body = json.writeValueAsString(result);
        assertTrue(body.contains("公开测试企划"));
        for (String forbidden : List.of("991", "private-image-marker", "private-moderation-marker", "moderatedBy", "employerId")) assertFalse(body.contains(forbidden));
        verifyNoInteractions(projects, artists, users, reviews);
        verify(projectMapper).selectById(11L); verifyNoMoreInteractions(projectMapper);
    }
    @Test void directedHiddenDeletedExpiredAndModeratedProjectsAreAllUnavailable() {
        List<Project> cases = new ArrayList<>();
        var p = project(); p.setMode(1); cases.add(p);
        p = project(); p.setStatus(4); cases.add(p);
        p = project(); p.setDeleted(1); cases.add(p);
        p = project(); p.setModerationStatus(1); cases.add(p);
        p = project(); p.setDeadline(LocalDate.now().minusDays(1)); cases.add(p);
        p = project(); p.setDeadline(null); cases.add(p);
        cases.add(null);
        for (Project hidden : cases) {
            when(projectMapper.selectById(11L)).thenReturn(hidden);
            assertThrows(BusinessException.class, () -> reader.project(11));
            assertThrows(BusinessException.class, () -> reader.requireVisible("project", 11));
        }
        verifyNoInteractions(projects, artists, users, reviews);
    }
    @Test void searchIsFixedToFivePublicCandidatesWithHonestPaging() {
        var visible = project(); var hidden = project(); hidden.setMode(1); hidden.setTitle("should-not-leak");
        var page = new Page<Project>(1, 5, 8); page.setRecords(List.of(visible, hidden));
        when(projects.listProjects(1, 5, null, null, "latest", null, null, null, null, null, "头像", null)).thenReturn(page);
        var result = reader.projects(1, null, "头像", null, null, null);
        assertEquals(1, result.references().stream().filter(r -> r.kind().equals("project")).count());
        var data = (GuidePublicReader.SearchResult) result.data(); assertTrue(data.hasMore()); assertEquals(5, data.pageSize());
        verify(projects).listProjects(1, 5, null, null, "latest", null, null, null, null, null, "头像", null);
        verifyNoMoreInteractions(projects); verifyNoInteractions(projectMapper, users, reviews);
    }
    @Test void unverifiedDisabledAndDeletedArtistsCannotReachWorksOrReviews() {
        for (int state = 0; state < 3; state++) {
            var u = artist(); if (state == 0) u.setCertStatus(1); if (state == 1) u.setStatus(1); if (state == 2) u.setDeleted(1);
            when(users.selectById(21L)).thenReturn(u);
            assertThrows(BusinessException.class, () -> reader.artist(21));
        }
        verifyNoInteractions(artists, reviews, projects, projectMapper);
    }
    @Test void artistDetailTrimsPrivateWorkAndReviewFieldsWithoutSendingImages() throws Exception {
        var user = artist(); when(users.selectById(21L)).thenReturn(user);
        var summary = new ArtistSummary(); summary.setId(21L); summary.setNickname("公开画师"); summary.setAvatar("avatar-marker");
        var work = new Work(); work.setVisibility(1); work.setDeleted(0); work.setTitle("公开作品"); work.setImage("work-image-marker");
        var privateWork = new Work(); privateWork.setVisibility(0); privateWork.setDeleted(0); privateWork.setTitle("private-work-marker");
        summary.setWorks(List.of(work, privateWork)); when(artists.buildSummaries(List.of(user))).thenReturn(List.of(summary));
        var review = new Review(); review.setDeleted(0); review.setScore(5); review.setContent("画得很认真"); review.setOrderId(88881L); review.setFromId(77771L);
        var page = new Page<Review>(1, 3, 1); page.setRecords(List.of(review)); when(reviews.selectPublicPage(any(), eq(21L), eq("painter"))).thenReturn(page);
        String body = json.writeValueAsString(reader.artist(21));
        assertTrue(body.contains("公开作品") && body.contains("画得很认真"));
        for (String forbidden : List.of("private-work-marker", "work-image-marker", "avatar-marker", "secret-hash-marker", "88881", "77771", "orderId", "fromId")) assertFalse(body.contains(forbidden));
        verify(reviews).selectPublicPage(any(), eq(21L), eq("painter")); verifyNoMoreInteractions(reviews);
    }
    @Test void publicToolRejectsArbitraryActorUrlsSqlAndInvalidFilters() throws Exception {
        var tools = new GuideToolRegistry(json, new GuideRuleBook(json), reader);
        for (String args : List.of("{\"operation\":\"artist_detail\",\"id\":0}", "{\"operation\":\"project_detail\",\"id\":1,\"actorId\":3}",
                "{\"operation\":\"search_artists\",\"url\":\"http://localhost\"}", "{\"operation\":\"search_projects\",\"category\":\"任意类型\"}",
                "{\"operation\":\"search_projects\",\"page\":11}", "{\"operation\":\"search_projects\",\"budgetMin\":300,\"budgetMax\":100}",
                "{\"operation\":\"search_projects\",\"keyword\":\"%\"}", "{\"operation\":\"search_artists\",\"budgetMin\":10}"))
            assertThrows(GuideModelException.class, () -> tools.execute(new GuideModelClient.ToolCall("x", "read_artiv", args), new GuideCancellation()));
        verifyNoInteractions(projects, artists, users, reviews, projectMapper);
    }
    @Test void serverSearchOptionsMatchExistingFrontendEnums() throws Exception {
        var options = json.readTree(getClass().getResourceAsStream("/guide/search-options.json"));
        String frontend = Files.readString(Path.of("../frontend/src/constants/subjects.js"));
        var matcher = java.util.regex.Pattern.compile("'([^']+)'").matcher(frontend.substring(frontend.indexOf("export const SUBJECT_OPTIONS")));
        var literals = new ArrayList<String>(); while (matcher.find()) literals.add(matcher.group(1));
        var expected = new ArrayList<String>(); options.path("subjects").forEach(n -> expected.add(n.asText())); options.path("categories").forEach(n -> expected.add(n.asText()));
        // 注释里的 category 等文字不作为引号枚举，只比较数组声明中的值。
        assertEquals(new HashSet<>(expected), new HashSet<>(literals));
    }
}
