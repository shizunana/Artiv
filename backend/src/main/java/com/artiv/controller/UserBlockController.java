package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.BlockStatusResponse;
import com.artiv.dto.BlockedUserItem;
import com.artiv.security.UserContext;
import com.artiv.service.UserBlockService;
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
@RequestMapping("/api/v1/blocks/users")
@RequiredArgsConstructor
public class UserBlockController {
    private final UserBlockService userBlockService;

    @GetMapping
    public Result<Page<BlockedUserItem>> list(@RequestParam(defaultValue = "1") long page,
                                              @RequestParam(defaultValue = "10") long size) {
        return Result.ok(userBlockService.listMine(page, size, UserContext.getUserId()));
    }

    @GetMapping("/{userId}/status")
    public Result<BlockStatusResponse> status(@PathVariable Long userId) {
        return Result.ok(userBlockService.getStatus(UserContext.getUserId(), userId));
    }

    @PutMapping("/{userId}")
    public Result<BlockStatusResponse> block(@PathVariable Long userId) {
        return Result.ok(userBlockService.block(UserContext.getUserId(), userId));
    }

    @DeleteMapping("/{userId}")
    public Result<BlockStatusResponse> unblock(@PathVariable Long userId) {
        return Result.ok(userBlockService.unblock(UserContext.getUserId(), userId));
    }
}
