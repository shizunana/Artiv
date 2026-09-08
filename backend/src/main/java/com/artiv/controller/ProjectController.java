package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.common.BusinessException;
import com.artiv.common.ResultCode;
import jakarta.servlet.http.HttpServletRequest;
import com.artiv.dto.ProjectRequest;
import com.artiv.dto.ProjectContentUpdateRequest;
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
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

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
                                      @RequestParam(required = false) String projectCategory,
                                      @RequestParam(required = false) java.util.List<String> projectCategories,
                                      @RequestParam(required = false) String subject,
                                      @RequestParam(required = false) java.util.List<String> subjects,
                                      HttpServletRequest httpRequest) {
        if (httpRequest.getParameterMap().containsKey("category")) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "企划字段 category 已停用，请刷新页面后使用 projectCategory");
        }
        if (httpRequest.getParameterMap().containsKey("subject") && httpRequest.getParameterMap().containsKey("subjects")) {
            throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "subject 与 subjects 不能同时传入");
        }
        if (httpRequest.getParameterMap().containsKey("projectCategories")) {
            if (httpRequest.getParameterMap().containsKey("projectCategory")) {
                throw new BusinessException(ResultCode.PARAM_INVALID.getCode(), "projectCategory 与 projectCategories 不能同时传入");
            }
            var categories = com.artiv.common.ProjectSubjectRules.categories(projectCategories);
            if (httpRequest.getParameterMap().containsKey("subjects")) {
                var checkedSubjects = com.artiv.common.ProjectSubjectRules.queries(categories, subjects);
                return Result.ok(projectService.listProjectsByCategoriesAndSubjects(page, size, keyword, status, sort,
                        budgetMin, budgetMax, deadlineWithin, deadlineAfter, tag, categories, checkedSubjects));
            }
            String checkedSubject = com.artiv.common.ProjectSubjectRules.query(categories, subject);
            return Result.ok(projectService.listProjectsByCategories(page, size, keyword, status, sort,
                    budgetMin, budgetMax, deadlineWithin, deadlineAfter, tag, categories, checkedSubject));
        }
        if (httpRequest.getParameterMap().containsKey("subjects")) {
            var category = com.artiv.common.ProjectCategoryRules.optional(projectCategory);
            var categories = category == null ? java.util.List.<String>of() : java.util.List.of(category);
            var checkedSubjects = com.artiv.common.ProjectSubjectRules.queries(categories, subjects);
            return Result.ok(projectService.listProjectsByCategoriesAndSubjects(page, size, keyword, status, sort,
                    budgetMin, budgetMax, deadlineWithin, deadlineAfter, tag, categories, checkedSubjects));
        }
        return Result.ok(projectService.listProjects(page, size, keyword, status, sort,
                budgetMin, budgetMax, deadlineWithin, deadlineAfter, tag, projectCategory, subject));
    }

    /** 我的企划（企划方工作台） */
    @GetMapping("/mine")
    public Result<Page<Project>> mine(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) Integer mode) {
        return Result.ok(projectService.listMine(page, size, UserContext.getUserId(), mode));
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

    /** 发布者专属企划面板摘要。 */
    @GetMapping("/{id}/panel")
    public Result<Project> panel(@PathVariable Long id) {
        return Result.ok(projectService.getPanel(id, UserContext.getUserId()));
    }

    /** 仅在企划尚未形成有效应征、有效邀请或任何历史订单时允许完整编辑内容。 */
    @PutMapping("/{id}/content")
    public Result<Project> updateContent(@PathVariable Long id,
                                         @Valid @RequestBody ProjectContentUpdateRequest request) {
        return Result.ok(projectService.updateContent(id, request, UserContext.getUserId()));
    }

    /** 仅修改企划截稿日期；过期或下架企划也允许修改。 */
    @PutMapping("/{id}/deadline")
    public Result<Project> updateDeadline(@PathVariable Long id,
                                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline) {
        return Result.ok(projectService.updateDeadline(id, deadline, UserContext.getUserId()));
    }

    /** 发布者上架/下架企划：0 招募中，4 已下架；重新上架必须同时选择新截稿日期。 */
    @PutMapping("/{id}/status")
    public Result<Project> changeStatus(@PathVariable Long id,
                                        @RequestParam Integer status,
                                        @RequestParam(required = false)
                                        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline) {
        return Result.ok(projectService.changeStatus(id, status, deadline, UserContext.getUserId()));
    }

    /** 被拒绝的定向企划改为公开招募企划。 */
    @PutMapping("/{id}/publish-public")
    public Result<Project> publishPublic(@PathVariable Long id,
                                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate deadline) {
        return Result.ok(projectService.publishPublic(id, deadline, UserContext.getUserId()));
    }

    /** 仅无任何关联订单的企划允许删除。 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        projectService.delete(id, UserContext.getUserId());
        return Result.ok();
    }
}
