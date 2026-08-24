package com.artiv.controller;

import com.artiv.common.Result;
import com.artiv.dto.FavoriteStallItem;
import com.artiv.dto.FavoriteStateResponse;
import com.artiv.dto.FavoriteWorkItem;
import com.artiv.security.UserContext;
import com.artiv.service.FavoriteService;
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
@RequestMapping("/api/v1/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    @GetMapping("/works")
    public Result<Page<FavoriteWorkItem>> works(@RequestParam(defaultValue = "1") long page,
                                                @RequestParam(defaultValue = "12") long size) {
        return Result.ok(favoriteService.listWorks(page, size, UserContext.getUserId()));
    }

    @GetMapping("/works/{workId}/status")
    public Result<FavoriteStateResponse> workStatus(@PathVariable Long workId) {
        return Result.ok(favoriteService.getWorkState(workId, UserContext.getUserId()));
    }

    @PutMapping("/works/{workId}")
    public Result<FavoriteStateResponse> favoriteWork(@PathVariable Long workId) {
        return Result.ok(favoriteService.favoriteWork(workId, UserContext.getUserId()));
    }

    @DeleteMapping("/works/{workId}")
    public Result<FavoriteStateResponse> unfavoriteWork(@PathVariable Long workId) {
        return Result.ok(favoriteService.unfavoriteWork(workId, UserContext.getUserId()));
    }

    @GetMapping("/stalls")
    public Result<Page<FavoriteStallItem>> stalls(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "12") long size) {
        return Result.ok(favoriteService.listStalls(page, size, UserContext.getUserId()));
    }

    @GetMapping("/stalls/{stallId}/status")
    public Result<FavoriteStateResponse> stallStatus(@PathVariable Long stallId) {
        return Result.ok(favoriteService.getStallState(stallId, UserContext.getUserId()));
    }

    @PutMapping("/stalls/{stallId}")
    public Result<FavoriteStateResponse> favoriteStall(@PathVariable Long stallId) {
        return Result.ok(favoriteService.favoriteStall(stallId, UserContext.getUserId()));
    }

    @DeleteMapping("/stalls/{stallId}")
    public Result<FavoriteStateResponse> unfavoriteStall(@PathVariable Long stallId) {
        return Result.ok(favoriteService.unfavoriteStall(stallId, UserContext.getUserId()));
    }
}
