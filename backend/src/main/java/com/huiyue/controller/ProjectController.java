package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.dto.ProjectRequest;
import com.huiyue.entity.Project;
import com.huiyue.security.UserContext;
import com.huiyue.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public Result<Page<Project>> list(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status,
                                      @RequestParam(required = false) String sort,
                                      @RequestParam(required = false) BigDecimal budgetMin,
                                      @RequestParam(required = false) BigDecimal budgetMax,
                                      @RequestParam(required = false) Integer deadlineWithin,
                                      @RequestParam(required = false) Integer deadlineAfter,
                                      @RequestParam(required = false) String tag) {
        return Result.ok(projectService.listProjects(page, size, keyword, status, sort,
                budgetMin, budgetMax, deadlineWithin, deadlineAfter, tag));
    }

    /** 我的企划（企划方工作台） */
    @GetMapping("/mine")
    public Result<Page<Project>> mine(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(projectService.listMine(page, size, UserContext.getUserId()));
    }

    /** 发布企划（仅企划方） */
    @PostMapping
    public Result<Project> create(@Valid @RequestBody ProjectRequest request) {
        return Result.ok(projectService.create(request, UserContext.getUserId()));
    }

    /** 企划详情 */
    @GetMapping("/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        return Result.ok(projectService.getById(id));
    }
}
