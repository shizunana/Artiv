package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.ApplicationDetail;
import com.artiv.dto.ApplicationQuoteUpdateRequest;
import com.artiv.dto.MyApplicationItem;
import com.artiv.entity.Application;
import com.artiv.security.UserContext;
import com.artiv.service.ApplicationService;
import com.artiv.service.MessageService;
import com.artiv.service.OrderService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
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
    private final OrderService orderService;

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
    public Result<Page<MyApplicationItem>> mine(@RequestParam(defaultValue = "1") long page,
                                                 @RequestParam(defaultValue = "10") long size) {
        return Result.ok(applicationService.listMine(page, size, UserContext.getUserId()));
    }

    @PutMapping("/applications/{id}/select")
    public Result<Application> select(@PathVariable Long id) {
        return Result.ok(applicationService.select(id, UserContext.getUserId()));
    }

    @PutMapping("/applications/{id}/quote")
    public Result<Application> updateQuote(@PathVariable Long id,
                                           @Valid @RequestBody ApplicationQuoteUpdateRequest request) {
        return Result.ok(applicationService.updateQuote(id, UserContext.getUserId(), request.getQuote()));
    }

    @PostMapping("/projects/{projectId}/conversations/{painterId}")
    public Result<com.artiv.entity.Message> startConversation(@PathVariable Long projectId,
                                                                @PathVariable Long painterId) {
        return Result.ok(messageService.startProjectConversation(UserContext.getUserId(), projectId, painterId));
    }

    @PostMapping("/projects/{projectId}/applications/{applicationId}/order")
    public Result<com.artiv.entity.OrderInfo> createProjectOrder(@PathVariable Long projectId,
                                                                  @PathVariable Long applicationId) {
        return Result.ok(orderService.createProjectOrder(projectId, applicationId, UserContext.getUserId()));
    }

    @DeleteMapping("/applications/{id}")
    public Result<Void> withdraw(@PathVariable Long id) {
        applicationService.withdraw(id, UserContext.getUserId());
        return Result.ok();
    }
}
