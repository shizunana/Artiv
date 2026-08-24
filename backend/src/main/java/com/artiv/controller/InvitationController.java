package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.InvitationAcceptRequest;
import com.artiv.dto.InvitationProjectSummary;
import com.artiv.dto.InvitationRejectRequest;
import com.artiv.entity.Invitation;
import com.artiv.security.UserContext;
import com.artiv.service.InvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final InvitationService invitationService;

    @GetMapping
    public Result<Page<InvitationProjectSummary>> mine(@RequestParam(defaultValue = "1") long page,
                                                        @RequestParam(defaultValue = "10") long size) {
        return Result.ok(invitationService.listMine(page, size, UserContext.getUserId()));
    }

    @GetMapping("/project/{projectId}")
    public Result<InvitationProjectSummary> mineByProject(@PathVariable Long projectId) {
        return Result.ok(invitationService.getMineByProject(projectId, UserContext.getUserId()));
    }

    @PostMapping("/{id}/accept")
    public Result<Invitation> accept(@PathVariable Long id,
                                     @Valid @RequestBody InvitationAcceptRequest request) {
        return Result.ok(invitationService.accept(id, request, UserContext.getUserId()));
    }

    @PostMapping("/{id}/reject")
    public Result<Invitation> reject(@PathVariable Long id,
                                     @Valid @RequestBody InvitationRejectRequest request) {
        return Result.ok(invitationService.reject(id, request, UserContext.getUserId()));
    }
}
