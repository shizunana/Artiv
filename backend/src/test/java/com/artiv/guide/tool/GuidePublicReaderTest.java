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
    StallService stalls = mock(StallService.class);
    ProjectMapper projectMapper = mock(ProjectMapper.class); StallMapper stallMapper = mock(StallMapper.class);
    UserMapper users = mock(UserMapper.class); ReviewMapper reviews = mock(ReviewMapper.class);
    ObjectMapper json = new ObjectMapper();
    GuidePublicReader reader = new GuidePublicReader(projects, artists, stalls, projectMapper, stallMapper, users, reviews, json);
    Project project() {
        var p = new Project(); p.setId(11L); p.setMode(0); p.setStatus(0); p.setDeleted(0); p.setModerationStatus(0);
        p.setDeadline(LocalDate.now()); p.setTitle("公开测试企划"); p.setEmployerId(991L); p.setImages("private-image-marker"); p.setModerationReason("private-moderation-marker"); return p;
    }
    User artist() {
        var u = new User(); u.setId(21L); u.setStatus(0); u.setDeleted(0); u.setCertStatus(2); u.setPassword("secret-hash-marker"); return u;
    }
    Stall stall() {
        var s = new Stall(); s.setId(31L); s.setStatus(1); s.setDeleted(0); s.setStock(2); s.setTitle("公开头像橱窗");
        s.setCategory("头像"); s.setParams("{\"subject\":[\"Q版\",\"日系\"],\"private\":\"not-returned\"}");
        s.setDescription("可约头像"); s.setPrice(new java.math.BigDecimal("128")); s.setDeliverTime("7天"); s.setCover("private-cover-marker");
        s.setImages("private-images-marker"); s.setPainterId(991L); s.setPainterName("公开画师"); return s;
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
    @Test void publicCardsAreStructuredAndHandleMissingOrSingleEndedBudgets() throws Exception {
        var p = project(); p.setProjectCategory("头像"); p.setSubject("日系");
        p.setBudgetMin(new java.math.BigDecimal("290.00")); p.setBudgetMax(new java.math.BigDecimal("7100.00"));
        when(projectMapper.selectById(11L)).thenReturn(p);
        var ref = reader.project(11).references().getFirst();
        assertEquals("头像", ref.card().category());
        assertEquals(List.of(new GuideReference.Field("预算", "290～7100 元"),
                new GuideReference.Field("题材", "日系"), new GuideReference.Field("截稿", LocalDate.now().toString())), ref.card().fields());
        assertEquals(ref, json.readValue(json.writeValueAsString(ref), GuideReference.class));
        p.setBudgetMin(null);
        assertEquals("最高 7100 元", reader.project(11).references().getFirst().card().fields().getFirst().value());
        p.setBudgetMax(null);
        assertEquals("未提供", reader.project(11).references().getFirst().card().fields().getFirst().value());
        p.setBudgetMin(new java.math.BigDecimal("100"));
        assertEquals("最低 100 元", reader.project(11).references().getFirst().card().fields().getFirst().value());
        p.setBudgetMax(new java.math.BigDecimal("100"));
        assertEquals("100 元", reader.project(11).references().getFirst().card().fields().getFirst().value());
        assertNull(json.readValue("{\"kind\":\"project\",\"id\":\"11\",\"title\":\"旧引用\"}", GuideReference.class).card());
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
    @Test void projectCoversSupportBothStoredFormatsButNeverEnterModelData() throws Exception {
        var p = project();
        when(projectMapper.selectById(11L)).thenReturn(p);
        for (String images : List.of("[\"/uploads/first.png\",\"/uploads/second.png\"]", " /uploads/first.png, /uploads/second.png")) {
            p.setImages(images);
            var result = reader.project(11);
            assertEquals("/uploads/first.png", result.references().getFirst().card().coverUrl());
            assertFalse(json.writeValueAsString(result.data()).contains("/uploads/"));
            assertFalse(json.writeValueAsString(result.references()).contains("second.png"));
            var page = new Page<Project>(1, 5, 1); page.setRecords(List.of(p));
            when(projects.listProjects(1, 5, null, null, "latest", null, null, null, null, null, null, null)).thenReturn(page);
            assertEquals("/uploads/first.png", reader.projects(1, null, null, null, null, null).references().get(1).card().coverUrl());
        }
        for (String images : List.of("[]", "javascript:alert(1)", "/api/v1/admin/private", "data:image/png;base64,abc", "//evil.example/a.png")) {
            p.setImages(images);
            assertNull(reader.project(11).references().getFirst().card().coverUrl());
        }
    }
    @Test void stallCoverAndPainterAvatarStayInReferencesForSearchAndDetail() throws Exception {
        var s = stall(); s.setCover("/uploads/stall.png");
        s.setPainterAvatar("/uploads/avatar.png");
        var page = new Page<Stall>(1, 5, 1); page.setRecords(List.of(s));
        when(stalls.listStalls(1, 5, null, null, "latest", null, null, null, null)).thenReturn(page);
        var result = reader.stalls(1, null, null, null, null, null);
        assertEquals("/uploads/stall.png", result.references().get(1).card().coverUrl());
        assertEquals("/uploads/avatar.png", result.references().get(1).card().avatarUrl());
        assertFalse(json.writeValueAsString(result.data()).contains("/uploads/"));
        var painter = artist(); painter.setNickname("详情画师"); painter.setAvatar("/uploads/detail-avatar.png");
        when(users.selectById(991L)).thenReturn(painter);
        when(stallMapper.selectById(31L)).thenReturn(s);
        result = reader.stall(31);
        assertEquals("详情画师", result.references().getFirst().card().creatorName());
        assertEquals("/uploads/detail-avatar.png", result.references().getFirst().card().avatarUrl());
        assertFalse(json.writeValueAsString(result.data()).contains("/uploads/"));
    }
    @Test void artistSearchReturnsVerifiedArtistCardsWithOnlyPublicArtworkPreview() throws Exception {
        var summary = new ArtistSummary(); summary.setId(21L); summary.setNickname("头像画师"); summary.setAvatar("/uploads/artist.png");
        var hidden = new Work(); hidden.setDeleted(0); hidden.setVisibility(0); hidden.setImage("/uploads/hidden.png");
        var deleted = new Work(); deleted.setDeleted(1); deleted.setVisibility(1); deleted.setImage("/uploads/deleted.png");
        var visible = new Work(); visible.setDeleted(0); visible.setVisibility(1); visible.setImage("/uploads/work.png");
        summary.setWorks(List.of(hidden, deleted, visible));
        var rejected = new ArtistSummary(); rejected.setId(22L); rejected.setNickname("不可推荐");
        var page = new Page<ArtistSummary>(1, 5, 2); page.setRecords(List.of(summary, rejected));
        when(artists.listGuideArtists(1, 5, "头像", "日系", null)).thenReturn(page);
        when(users.selectById(21L)).thenReturn(artist());
        var tools = new GuideToolRegistry(json, new GuideRuleBook(json), reader);
        var result = tools.execute(new GuideModelClient.ToolCall("a", "read_artiv",
                "{\"operation\":\"search_artists\",\"category\":\"头像\",\"subject\":\"日系\"}"), new GuideCancellation());
        assertEquals(2, result.references().size());
        var ref = result.references().get(1);
        assertEquals("artist", ref.kind()); assertEquals("头像画师", ref.title());
        assertEquals("/uploads/artist.png", ref.card().avatarUrl());
        assertEquals("/uploads/work.png", ref.card().coverUrl());
        assertFalse(result.content().contains("/uploads/"));
        for (String marker : List.of("hidden.png", "deleted.png", "不可推荐")) assertFalse(json.writeValueAsString(result).contains(marker));
    }
    @Test void stallProjectionUsesPublicHallVisibilityAndOmitsImagesAndInternalParams() throws Exception {
        when(stallMapper.selectById(31L)).thenReturn(stall());
        String body = json.writeValueAsString(reader.stall(31));
        assertTrue(body.contains("公开头像橱窗") && body.contains("Q版") && body.contains("128"));
        var card = json.readTree(body).path("references").get(0).path("card");
        assertEquals("头像", card.path("category").asText());
        assertEquals("128 元", card.path("fields").get(0).path("value").asText());
        for (String forbidden : List.of("private-cover-marker", "private-images-marker", "not-returned", "painterId", "991")) assertFalse(body.contains(forbidden));
        verify(stallMapper).selectById(31L); verifyNoMoreInteractions(stallMapper);
    }
    @Test void offShelfSoldOutDeletedAndMissingStallsAreUnavailable() {
        List<Stall> cases = new ArrayList<>();
        var s = stall(); s.setStatus(0); cases.add(s);
        s = stall(); s.setStock(0); cases.add(s);
        s = stall(); s.setDeleted(1); cases.add(s);
        cases.add(null);
        for (Stall hidden : cases) {
            when(stallMapper.selectById(31L)).thenReturn(hidden);
            assertThrows(BusinessException.class, () -> reader.stall(31));
            assertThrows(BusinessException.class, () -> reader.requireVisible("stall", 31));
        }
    }
    @Test void stallSearchReusesPublicHallFiltersAndReturnsStallReferences() {
        var visible = stall(); var hidden = stall(); hidden.setStock(0); hidden.setTitle("should-not-leak");
        var page = new Page<Stall>(1, 5, 7); page.setRecords(List.of(visible, hidden));
        when(stalls.listStalls(1, 5, "头像", "头像", "latest", "Q版", new java.math.BigDecimal("100"), new java.math.BigDecimal("200"), null)).thenReturn(page);
        var result = reader.stalls(1, "头像", "头像", "Q版", new java.math.BigDecimal("100"), new java.math.BigDecimal("200"));
        assertEquals(1, result.references().stream().filter(r -> r.kind().equals("stall")).count());
        var data = (GuidePublicReader.SearchResult) result.data(); assertTrue(data.hasMore()); assertEquals(5, data.pageSize());
        assertNull(result.references().getFirst().card()); // 查询元信息不是资源卡片。
        verify(stalls).listStalls(1, 5, "头像", "头像", "latest", "Q版", new java.math.BigDecimal("100"), new java.math.BigDecimal("200"), null);
        verifyNoMoreInteractions(stalls); verifyNoInteractions(stallMapper);
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
        var summary = new ArtistSummary(); summary.setId(21L); summary.setNickname("公开画师"); summary.setAvatar("/uploads/avatar.png");
        var work = new Work(); work.setVisibility(1); work.setDeleted(0); work.setTitle("公开作品"); work.setImage("/uploads/work.png");
        var privateWork = new Work(); privateWork.setVisibility(0); privateWork.setDeleted(0); privateWork.setTitle("private-work-marker");
        summary.setWorks(List.of(work, privateWork)); when(artists.buildGuideSummaries(List.of(user))).thenReturn(List.of(summary));
        var review = new Review(); review.setDeleted(0); review.setScore(5); review.setContent("画得很认真"); review.setOrderId(88881L); review.setFromId(77771L);
        var page = new Page<Review>(1, 3, 1); page.setRecords(List.of(review)); when(reviews.selectPublicPage(any(), eq(21L), eq("painter"))).thenReturn(page);
        var result = reader.artist(21);
        String body = json.writeValueAsString(result);
        assertEquals("/uploads/avatar.png", result.references().getFirst().card().avatarUrl());
        assertEquals("/uploads/work.png", result.references().getFirst().card().coverUrl());
        assertFalse(json.writeValueAsString(result.data()).contains("/uploads/"));
        assertTrue(body.contains("公开作品") && body.contains("画得很认真"));
        assertTrue(json.readTree(body).path("references").get(0).path("card").isObject());
        for (String forbidden : List.of("private-work-marker", "work-image-marker", "avatar-marker", "secret-hash-marker", "88881", "77771", "orderId", "fromId")) assertFalse(body.contains(forbidden));
        verify(reviews).selectPublicPage(any(), eq(21L), eq("painter")); verifyNoMoreInteractions(reviews);
    }
    @Test void categoryBatchDeduplicatesArtistsAndKeepsMissingCategoryEvidence() {
        var a = new ArtistSummary(); a.setId(21L); a.setNickname("多类型画师");
        var found = new Page<ArtistSummary>(1, 1, 3); found.setRecords(List.of(a));
        when(artists.listGuideArtists(1, 1, "头像", null, null)).thenReturn(found);
        when(artists.listGuideArtists(1, 1, "立绘", null, null)).thenReturn(found);
        when(artists.listGuideArtists(1, 1, "Live2d", null, null)).thenReturn(new Page<>(1, 1, 0));
        when(users.selectById(21L)).thenReturn(artist());
        var result = reader.artistsByCategory(List.of("头像", "立绘", "Live2d"), null, new GuideCancellation());
        assertEquals(1, result.references().stream().filter(r -> "artist".equals(r.kind())).count());
        var groups = json.valueToTree(result.data()).path("groups");
        assertEquals(3, groups.size()); assertEquals("21", groups.get(0).path("artistId").asText());
        assertEquals("21", groups.get(1).path("artistId").asText()); assertFalse(groups.get(2).path("matched").asBoolean());
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
