package com.artiv.guide.tool;

import com.artiv.common.BusinessException;
import com.artiv.dto.ArtistSummary;
import com.artiv.entity.*;
import com.artiv.mapper.*;
import com.artiv.service.ArtistService;
import com.artiv.service.ProjectService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

/** 只复用已审查的公开查询。禁止调用邀请查询、订单详情或管理员宽权限详情方法。 */
@Component
public class GuidePublicReader {
    public record ReadResult(Object data, List<GuideReference> references) { }
    public record ProjectCard(String id, String title, String category, String subject, String description,
                              BigDecimal budgetMin, BigDecimal budgetMax, String deadline) { }
    public record WorkText(String title, String category, String subject, String description) { }
    public record ArtistCard(String id, String name, String bio, String tags, List<String> categories,
                             List<WorkText> publicWorks, Double averageScore, long reviewCount) { }
    public record ReviewText(Integer score, String content, String date) { }
    public record SearchResult(String scope, long page, long pageSize, long total, boolean hasMore, String checkedAt, Object records) { }
    private final ProjectService projects;
    private final ArtistService artists;
    private final ProjectMapper projectMapper;
    private final UserMapper userMapper;
    private final ReviewMapper reviewMapper;
    public GuidePublicReader(ProjectService projects, ArtistService artists, ProjectMapper projectMapper, UserMapper userMapper, ReviewMapper reviewMapper) {
        this.projects = projects; this.artists = artists; this.projectMapper = projectMapper;
        this.userMapper = userMapper; this.reviewMapper = reviewMapper;
    }
    public ReadResult projects(int page, String keyword, String category, String subject, BigDecimal minimum, BigDecimal maximum) {
        var result = projects.listProjects(page, 5, keyword, null, "latest", minimum, maximum, null, null, null, category, subject);
        // 公共服务已筛选；再次检查可见性，避免未来复用方法扩权后向导意外泄露。
        var cards = result.getRecords().stream().filter(this::publicProject).limit(5).map(this::projectCard).toList();
        String now = Instant.now().toString();
        var refs = new ArrayList<>(cards.stream().map(c -> projectRef(c, now)).toList());
        refs.addFirst(new GuideReference("query", "projects-page-" + page, "公开企划查询", "第 " + page + " 页 · 每页最多5条 · 匹配总数 " + result.getTotal()
                + " · 本页返回 " + cards.size() + " 条；类型=" + Objects.toString(category, "不限") + "，题材=" + Objects.toString(subject, "不限")
                + "，标题关键词=" + Objects.toString(keyword, "不限") + "，预算=" + Objects.toString(minimum, "不限") + "～" + Objects.toString(maximum, "不限"), now));
        return new ReadResult(new SearchResult("公开招募中的企划；本页候选，非全库语义最佳排名；预算区间相交，关键词匹配标题", page, 5,
                result.getTotal(), result.getTotal() > (long) page * 5, now, cards), List.copyOf(refs));
    }
    public ReadResult project(long id) {
        var project = projectMapper.selectById(id);
        if (!publicProject(project)) throw unavailable();
        var card = projectCard(project); String now = Instant.now().toString();
        return new ReadResult(card, List.of(projectRef(card, now)));
    }
    public ReadResult artists(int page, String keyword, String category, String subjects) {
        var result = artists.listArtists(page, 5, category, subjects, keyword);
        var cards = result.getRecords().stream().limit(5)
                .filter(a -> publicArtist(userMapper.selectById(a.getId())))
                .map(this::artistCard).toList();
        String now = Instant.now().toString();
        var refs = new ArrayList<>(cards.stream().map(c -> artistRef(c, now)).toList());
        refs.addFirst(new GuideReference("query", "artists-page-" + page, "公开画师查询", "第 " + page + " 页 · 每页最多5条 · 匹配总数 " + result.getTotal()
                + " · 本页返回 " + cards.size() + " 条；类型=" + Objects.toString(category, "不限") + "，题材=" + Objects.toString(subjects, "不限")
                + "，关键词=" + Objects.toString(keyword, "不限") + "；按用户ID排序，非最佳排名", now));
        return new ReadResult(new SearchResult("正常认证画师；本页候选按用户ID排序，不是最佳排名；无统一报价、实时档期或授权范围", page, 5,
                result.getTotal(), result.getTotal() > (long) page * 5, now, cards), List.copyOf(refs));
    }
    public ReadResult artist(long id) {
        var user = userMapper.selectById(id);
        if (!publicArtist(user)) throw unavailable();
        var card = artistCard(artists.buildSummaries(List.of(user)).getFirst());
        // 复用公开评价 SQL，不调用会装载订单标题/图片/申诉信息的完整评价 DTO。
        var reviews = reviewMapper.selectPublicPage(new Page<>(1, 3), id, "painter");
        var texts = reviews.getRecords().stream().filter(r -> Integer.valueOf(0).equals(r.getDeleted()))
                .limit(3).map(r -> new ReviewText(r.getScore(), clip(r.getContent(), 300), String.valueOf(r.getCreatedAt()))).toList();
        String now = Instant.now().toString();
        return new ReadResult(Map.of("artist", card, "latestPublicReviews", texts, "reviewsTotal", reviews.getTotal(),
                "reviewScope", "最多3条近期公开评价，非全部评价；不提供订单或评价者身份", "checkedAt", now), List.of(artistRef(card, now)));
    }
    public void requireVisible(String kind, long id) {
        if ("project".equals(kind)) { if (!publicProject(projectMapper.selectById(id))) throw unavailable(); }
        else if ("artist".equals(kind)) { if (!publicArtist(userMapper.selectById(id))) throw unavailable(); }
        else throw unavailable();
    }
    private boolean publicProject(Project p) {
        return p != null && Integer.valueOf(0).equals(p.getDeleted()) && Integer.valueOf(0).equals(p.getMode())
                && Integer.valueOf(0).equals(p.getStatus()) && Integer.valueOf(0).equals(p.getModerationStatus())
                && p.getDeadline() != null && !p.getDeadline().isBefore(LocalDate.now());
    }
    private boolean publicArtist(User user) {
        return user != null && Integer.valueOf(0).equals(user.getDeleted()) && Integer.valueOf(0).equals(user.getStatus()) && Integer.valueOf(2).equals(user.getCertStatus());
    }
    private ProjectCard projectCard(Project p) {
        return new ProjectCard(String.valueOf(p.getId()), clip(p.getTitle(), 80), clip(p.getCategory(), 30), clip(p.getSubject(), 120),
                clip(p.getDescription(), 500), p.getBudgetMin(), p.getBudgetMax(), p.getDeadline().toString());
    }
    private ArtistCard artistCard(ArtistSummary a) {
        var works = (a.getWorks() == null ? List.<Work>of() : a.getWorks()).stream()
                .filter(w -> Integer.valueOf(0).equals(w.getDeleted()) && Integer.valueOf(1).equals(w.getVisibility()))
                .limit(3).map(w -> new WorkText(clip(w.getTitle(), 80), clip(w.getCategory(), 30), clip(w.getSubject(), 120), clip(w.getDescription(), 120))).toList();
        return new ArtistCard(String.valueOf(a.getId()), clip(a.getNickname() == null || a.getNickname().isBlank() ? a.getUsername() : a.getNickname(), 80),
                clip(a.getBio(), 300), clip(a.getTags(), 150), a.getCategories() == null ? List.of() : a.getCategories().stream().limit(10).map(c -> clip(c, 30)).toList(),
                works, a.getAverageScore(), a.getReviewCount());
    }
    private GuideReference projectRef(ProjectCard c, String now) {
        return new GuideReference("project", c.id(), c.title(), "公开企划 · " + c.category() + " · 预算 " + c.budgetMin() + "～" + c.budgetMax() + " 元 · 截稿 " + c.deadline(), now);
    }
    private GuideReference artistRef(ArtistCard c, String now) {
        return new GuideReference("artist", c.id(), c.name(), "认证画师 · " + String.join("、", c.categories()) + " · 公开评价 " + c.reviewCount() + " 条；报价与档期需另行确认", now);
    }
    private String clip(String value, int max) { return value == null ? "" : value.length() <= max ? value : value.substring(0, max) + "…"; }
    private BusinessException unavailable() { return new BusinessException(404, "该资源不存在或当前不在向导公开范围内"); }
}
