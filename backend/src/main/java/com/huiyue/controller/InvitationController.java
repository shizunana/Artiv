package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.entity.Invitation;
import com.huiyue.security.UserContext;
import com.huiyue.service.InvitationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    public Result<Page<Invitation>> mine(@RequestParam(defaultValue = "1") long page,
                                         @RequestParam(defaultValue = "10") long size) {
        return Result.ok(invitationService.listMine(page, size, UserContext.getUserId()));
    }

    @PostMapping
    public Result<Invitation> create(@RequestBody Invitation invitation) {
        return Result.ok(invitationService.create(invitation));
    }

    @PutMapping("/{id}/respond")
    public Result<Invitation> respond(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(invitationService.respond(id, status, UserContext.getUserId()));
    }
}
