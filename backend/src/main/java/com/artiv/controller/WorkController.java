package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.WorkRequest;
import com.artiv.dto.WorkDetailResponse;
import com.artiv.entity.Work;
import com.artiv.security.UserContext;
import com.artiv.service.WorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PutMapping;

@RestController
@RequestMapping("/api/v1/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    @GetMapping
    public Result<Page<Work>> list(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "12") long size,
                                   @RequestParam(required = false) String category,
                                   @RequestParam(required = false) String subject,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(defaultValue = "newest") String sort) {
        return Result.ok(workService.listWorks(
                page,
                size,
                UserContext.getUserId(),
                normalizeFilter(category),
                normalizeFilter(subject),
                normalizeFilter(keyword),
                sort
        ));
    }

    @GetMapping("/following")
    public Result<Page<Work>> following(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "12") long size,
                                        @RequestParam(required = false) String category,
                                        @RequestParam(required = false) String subject,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(workService.listFollowingWorks(
                page,
                size,
                UserContext.getUserId(),
                normalizeFilter(category),
                normalizeFilter(subject),
                normalizeFilter(keyword)
        ));
    }

    @GetMapping("/{id}")
    public Result<WorkDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(workService.getPublicDetail(id));
    }

    @GetMapping("/mine")
    public Result<Page<Work>> mine(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "12") long size) {
        return Result.ok(workService.listMine(page, size, UserContext.getUserId()));
    }

    @PostMapping
    public Result<Work> create(@Valid @RequestBody WorkRequest request) {
        Work work = new Work();
        BeanUtils.copyProperties(request, work);
        return Result.ok(workService.create(work, UserContext.getUserId()));
    }

    @PutMapping("/{id}/visibility")
    public Result<Work> changeVisibility(@PathVariable Long id, @RequestParam Integer visibility) {
        return Result.ok(workService.changeVisibility(id, visibility, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    private String normalizeFilter(String value) {
        return StringUtils.hasText(value) && !"all".equals(value) ? value.trim() : null;
    }
}
