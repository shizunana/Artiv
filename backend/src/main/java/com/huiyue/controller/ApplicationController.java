package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.dto.ApplicationDetail;
import com.huiyue.entity.Application;
import com.huiyue.security.UserContext;
import com.huiyue.service.ApplicationService;
import com.huiyue.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;
    private final MessageService messageService;

    @GetMapping("/projects/{projectId}/applications")
    public Result<Page<ApplicationDetail>> byProject(@PathVariable Long projectId,
                                                     @RequestParam(defaultValue = "1") long page,
                                                     @RequestParam(defaultValue = "10") long size) {
        return Result.ok(applicationService.listApplicantDetails(page, size, projectId, UserContext.getUserId()));
    }

    @PostMapping("/projects/{projectId}/applications")
    public Result<Application> create(@PathVariable Long projectId, @RequestBody Application application) {
        application.setProjectId(projectId);
        application.setPainterId(UserContext.getUserId());
        return Result.ok(applicationService.create(application));
    }

    @GetMapping("/applications/mine")
    public Result<Page<Application>> mine(@RequestParam(defaultValue = "1") long page,
                                          @RequestParam(defaultValue = "10") long size) {
        return Result.ok(applicationService.listMine(page, size, UserContext.getUserId()));
    }

    @PutMapping("/applications/{id}/select")
    public Result<Application> select(@PathVariable Long id) {
        return Result.ok(applicationService.select(id, UserContext.getUserId()));
    }

    @PostMapping("/projects/{projectId}/conversations/{painterId}")
    public Result<com.huiyue.entity.Message> startConversation(@PathVariable Long projectId,
                                                                @PathVariable Long painterId) {
        return Result.ok(messageService.startProjectConversation(UserContext.getUserId(), projectId, painterId));
    }

    @PostMapping("/projects/{projectId}/order-intents/{painterId}")
    public Result<String> createOrderIntent(@PathVariable Long projectId, @PathVariable Long painterId) {
        return Result.ok(messageService.createProjectOrderIntent(UserContext.getUserId(), projectId, painterId));
    }

    @DeleteMapping("/applications/{id}")
    public Result<Void> withdraw(@PathVariable Long id) {
        applicationService.withdraw(id, UserContext.getUserId());
        return Result.ok();
    }
}
