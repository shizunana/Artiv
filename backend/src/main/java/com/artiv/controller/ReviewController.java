package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.ReviewRequest;
import com.artiv.entity.Review;
import com.artiv.security.UserContext;
import com.artiv.service.ReviewService;
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

    @GetMapping("/reviews")
    public Result<Page<Review>> list(@RequestParam(defaultValue = "1") long page,
                                     @RequestParam(defaultValue = "10") long size,
                                     @RequestParam(required = false) Long userId) {
        return Result.ok(reviewService.listReviews(page, size, userId));
    }

    @PostMapping("/orders/{orderId}/review")
    public Result<Review> create(@PathVariable Long orderId, @Valid @RequestBody ReviewRequest request) {
        Review review = new Review();
        review.setOrderId(orderId);
        review.setFromId(UserContext.getUserId());
        review.setToId(request.getToId());
        review.setScore(request.getScore());
        review.setContent(request.getContent());
        return Result.ok(reviewService.create(review));
    }
}
