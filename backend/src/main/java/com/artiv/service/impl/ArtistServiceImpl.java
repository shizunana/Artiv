package com.artiv.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.artiv.dto.ArtistSummary;
import com.artiv.entity.User;
import com.artiv.entity.Work;
import com.artiv.mapper.UserMapper;
import com.artiv.mapper.WorkMapper;
import com.artiv.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ArtistServiceImpl implements ArtistService {

    private static final int PREVIEW_LIMIT = 5;

    private final UserMapper userMapper;
    private final WorkMapper workMapper;

    @Override
    public List<ArtistSummary> listArtists(String category, String subjects, String keyword) {
        List<User> painters = userMapper.selectList(new LambdaQueryWrapper<User>()
                .eq(User::getRole, 2)
                .eq(User::getCertStatus, 2)
                .eq(User::getStatus, 0)
                .eq(User::getDeleted, 0)
                .orderByAsc(User::getId));
        if (painters.isEmpty()) {
            return List.of();
        }

        List<Long> painterIds = painters.stream().map(User::getId).toList();
        List<Work> publicWorks = workMapper.selectList(new LambdaQueryWrapper<Work>()
                .in(Work::getUserId, painterIds)
                .eq(Work::getVisibility, 1)
                .eq(Work::getDeleted, 0)
                .orderByDesc(Work::getCreatedAt));
        Map<Long, List<Work>> worksByPainter = publicWorks.stream()
                .collect(Collectors.groupingBy(Work::getUserId));

        Set<String> requestedSubjects = splitValues(subjects);
        String normalizedKeyword = StringUtils.hasText(keyword)
                ? keyword.trim().toLowerCase(Locale.ROOT)
                : null;

        return painters.stream()
                .map(painter -> toSummary(painter, worksByPainter.getOrDefault(painter.getId(), List.of())))
                .filter(summary -> !StringUtils.hasText(category) || summary.getCategories().contains(category.trim()))
                .filter(summary -> requestedSubjects.isEmpty() || containsAllTags(summary.getTags(), requestedSubjects))
                .filter(summary -> normalizedKeyword == null || matchesKeyword(summary, normalizedKeyword))
                .toList();
    }

    private ArtistSummary toSummary(User painter, List<Work> works) {
        ArtistSummary summary = new ArtistSummary();
        summary.setId(painter.getId());
        summary.setUsername(painter.getUsername());
        summary.setNickname(painter.getNickname());
        summary.setAvatar(painter.getAvatar());
        summary.setBio(painter.getBio());
        summary.setTags(painter.getTags());
        summary.setCertStatus(painter.getCertStatus());
        summary.setCategories(works.stream()
                .map(Work::getCategory)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new))
                .stream().toList());
        summary.setWorks(new ArrayList<>(works.subList(0, Math.min(PREVIEW_LIMIT, works.size()))));
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

    private boolean containsAllTags(String tags, Set<String> requestedSubjects) {
        return splitValues(tags).containsAll(requestedSubjects);
    }

    private boolean matchesKeyword(ArtistSummary artist, String keyword) {
        return Stream.of(artist.getUsername(), artist.getNickname(), artist.getBio(), artist.getTags())
                .filter(StringUtils::hasText)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(value -> value.contains(keyword));
    }
}
