package com.artiv.controller;

import com.artiv.common.BusinessException;
import com.artiv.common.Result;
import com.artiv.common.ResultCode;
import com.artiv.dto.ReviewAppealRequest;
import com.artiv.dto.ReviewListItem;
import com.artiv.dto.ReviewRequest;
import com.artiv.entity.ReviewAppeal;
import com.artiv.security.UserContext;
import com.artiv.service.ReviewService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/users/{userId}/reviews")
    public Result<Page<ReviewListItem>> listPublic(@PathVariable Long userId,
                                                   @RequestParam(defaultValue = "painter") String role,
                                                   @RequestParam(defaultValue = "1") long page,
                                                   @RequestParam(defaultValue = "10") long size) {
        return Result.ok(reviewService.listPublic(page, size, userId, role, UserContext.getUserId()));
    }

    @PostMapping("/orders/{orderId}/reviews")
    public Result<ReviewListItem> create(@PathVariable Long orderId,
                                         @Valid @RequestBody ReviewRequest request) {
        return Result.ok(reviewService.create(orderId, UserContext.getUserId(), request));
    }

    @PostMapping("/reviews/{reviewId}/appeals")
    public Result<ReviewAppeal> appeal(@PathVariable Long reviewId,
                                       @Valid @RequestBody ReviewAppealRequest request) {
        return Result.ok(reviewService.submitAppeal(reviewId, UserContext.getUserId(), request));
    }

    @GetMapping("/reviews/{reviewId}/appeal")
    public Result<ReviewAppeal> appealStatus(@PathVariable Long reviewId) {
        return Result.ok(reviewService.getOwnAppeal(reviewId, UserContext.getUserId()));
    }

    /** @deprecated 使用 GET /users/{userId}/reviews，并显式提供主页身份。 */
    @Deprecated
    @GetMapping("/reviews")
    public Result<Page<ReviewListItem>> legacyList(@RequestParam(defaultValue = "1") long page,
                                                    @RequestParam(defaultValue = "10") long size,
                                                    @RequestParam(required = false) Long userId,
                                                    @RequestParam(defaultValue = "painter") String role) {
        if (userId == null) throw new BusinessException(ResultCode.PARAM_INVALID);
        return Result.ok(reviewService.listPublic(page, size, userId, role, UserContext.getUserId()));
    }

    /** @deprecated 使用复数路径 POST /orders/{orderId}/reviews。 */
    @Deprecated
    @PostMapping("/orders/{orderId}/review")
    public Result<ReviewListItem> legacyCreate(@PathVariable Long orderId,
                                               @Valid @RequestBody ReviewRequest request) {
        return Result.ok(reviewService.create(orderId, UserContext.getUserId(), request));
    }
}
