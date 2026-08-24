package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.ArtistCategoryAggregate;
import com.artiv.dto.ArtistSummary;
import com.artiv.dto.ReviewStatistics;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.ArtistService;
import com.artiv.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    private static final int PREVIEW_LIMIT = 5;

    private final UserMapper userMapper;
    private final WorkMapper workMapper;
    private final ReviewService reviewService;

    @Override
    public Page<ArtistSummary> listArtists(long page, long size, String category, String subjects, String keyword) {
        long safePage = Math.max(page, 1);
        long safeSize = Math.min(Math.max(size, 1), 50);
        Set<String> requestedSubjects = splitValues(subjects);
        String normalizedCategory = StringUtils.hasText(category) ? category.trim() : null;
        String normalizedKeyword = StringUtils.hasText(keyword) ? keyword.trim() : null;

        LambdaQueryWrapper<User> query = new LambdaQueryWrapper<User>()
                .eq(User::getCertStatus, 2)
                .eq(User::getStatus, 0)
                .eq(User::getDeleted, 0)
                .apply(normalizedCategory != null,
                        "EXISTS (SELECT 1 FROM work w WHERE w.user_id = `user`.id AND w.visibility = 1 AND w.deleted = 0 AND w.category = {0})",
                        normalizedCategory);
        requestedSubjects.forEach(subject -> query.apply(
                "FIND_IN_SET({0}, REPLACE(COALESCE(tags, ''), ' ', '')) > 0", subject));
        if (normalizedKeyword != null) {
            query.and(wrapper -> wrapper.like(User::getUsername, normalizedKeyword)
                    .or().like(User::getNickname, normalizedKeyword)
                    .or().like(User::getBio, normalizedKeyword)
                    .or().like(User::getTags, normalizedKeyword));
        }
        query.orderByAsc(User::getId);

        Page<User> painterPage = userMapper.selectPage(new Page<>(safePage, safeSize), query);
        List<User> painters = painterPage.getRecords();
        if (painters.isEmpty()) {
            return new Page<>(safePage, safeSize, painterPage.getTotal());
        }

        List<ArtistSummary> records = buildSummaries(painters);
        Page<ArtistSummary> result = new Page<>(safePage, safeSize, painterPage.getTotal());
        result.setRecords(records);
        return result;
    }

    @Override
    public List<ArtistSummary> buildSummaries(List<User> painters) {
        if (painters == null || painters.isEmpty()) {
            return List.of();
        }
        List<Long> painterIds = painters.stream().map(User::getId).toList();
        List<Work> publicWorks = workMapper.selectPublicPreviews(painterIds, PREVIEW_LIMIT);
        Map<Long, List<Work>> worksByPainter = publicWorks.stream()
                .collect(Collectors.groupingBy(Work::getUserId));
        Map<Long, List<String>> categoriesByPainter = workMapper.selectPublicCategories(painterIds).stream()
                .collect(Collectors.toMap(ArtistCategoryAggregate::getUserId,
                        aggregate -> new ArrayList<>(splitValues(aggregate.getCategories()))));
        Map<Long, ReviewStatistics> reviewStatistics = java.util.Optional.ofNullable(
                reviewService.getStatistics(painterIds, "painter")).orElse(Map.of());

        return painters.stream()
                .map(painter -> toSummary(painter, worksByPainter.getOrDefault(painter.getId(), List.of()),
                        categoriesByPainter.getOrDefault(painter.getId(), List.of()),
                        reviewStatistics.get(painter.getId())))
                .toList();
    }

    private ArtistSummary toSummary(User painter, List<Work> works, List<String> categories,
                                    ReviewStatistics reviewStatistics) {
        ArtistSummary summary = new ArtistSummary();
        summary.setId(painter.getId());
        summary.setUsername(painter.getUsername());
        summary.setNickname(painter.getNickname());
        summary.setAvatar(painter.getAvatar());
        summary.setBio(painter.getBio());
        summary.setTags(painter.getTags());
        summary.setCertStatus(painter.getCertStatus());
        summary.setCategories(categories);
        summary.setWorks(new ArrayList<>(works));
        summary.setAverageScore(reviewStatistics == null ? null : reviewStatistics.getAverageScore());
        summary.setReviewCount(reviewStatistics == null ? 0 : reviewStatistics.getReviewCount());
        return summary;
    }

    private Set<String> splitValues(String values) {
        if (!StringUtils.hasText(values)) {
            return Set.of();
        }
        return Arrays.stream(values.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

}
