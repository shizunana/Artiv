package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.ProjectRequest;
import com.artiv.entity.Project;
import com.artiv.security.UserContext;
import com.artiv.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
                                      @RequestParam(required = false) String tag,
                                      @RequestParam(required = false) String category,
                                      @RequestParam(required = false) String subject) {
        return Result.ok(projectService.listProjects(page, size, keyword, status, sort,
                budgetMin, budgetMax, deadlineWithin, deadlineAfter, tag, category, subject));
    }

    /** 我的企划（企划方工作台） */
    @GetMapping("/mine")
    public Result<Page<Project>> mine(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        return Result.ok(projectService.listMine(page, size, UserContext.getUserId()));
    }

    /** 发布企划：普通账号的注册主身份 role=1/2 不限制该能力。 */
    @PostMapping
    public Result<Project> create(@Valid @RequestBody ProjectRequest request) {
        return Result.ok(projectService.create(request, UserContext.getUserId()));
    }

    /** 企划详情 */
    @GetMapping("/{id}")
    public Result<Project> detail(@PathVariable Long id) {
        return Result.ok(projectService.getById(id, UserContext.getUserId()));
    }

    /** 发布者上架/下架企划：0 招募中，4 已下架。 */
    @PutMapping("/{id}/status")
    public Result<Project> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(projectService.changeStatus(id, status, UserContext.getUserId()));
    }

    /** 被拒绝的定向企划改为公开招募企划。 */
    @PutMapping("/{id}/publish-public")
    public Result<Project> publishPublic(@PathVariable Long id) {
        return Result.ok(projectService.publishPublic(id, UserContext.getUserId()));
    }

    /** 仅无任何关联订单的企划允许删除。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id, UserContext.getUserId());
        return Result.ok();
    }
}
