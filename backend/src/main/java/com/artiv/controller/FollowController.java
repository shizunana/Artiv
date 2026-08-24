package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.FollowStateResponse;
import com.artiv.dto.FollowedPainterItem;
import com.artiv.security.UserContext;
import com.artiv.service.FollowService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/follows/painters")
@RequiredArgsConstructor
public class FollowController {

    private final FollowService followService;

    @GetMapping
    public Result<Page<FollowedPainterItem>> list(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "12") long size) {
        return Result.ok(followService.listPainters(page, size, UserContext.getUserId()));
    }

    @GetMapping("/{painterId}/status")
    public Result<FollowStateResponse> status(@PathVariable Long painterId) {
        return Result.ok(followService.getPainterState(painterId, UserContext.getUserId()));
    }

    @PutMapping("/{painterId}")
    public Result<FollowStateResponse> follow(@PathVariable Long painterId) {
        return Result.ok(followService.followPainter(painterId, UserContext.getUserId()));
    }

    @DeleteMapping("/{painterId}")
    public Result<FollowStateResponse> unfollow(@PathVariable Long painterId) {
        return Result.ok(followService.unfollowPainter(painterId, UserContext.getUserId()));
    }
}
