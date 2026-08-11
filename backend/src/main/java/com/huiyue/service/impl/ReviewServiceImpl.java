package com.huiyue.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Review;
import com.huiyue.mapper.ReviewMapper;
import com.huiyue.service.ReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final ReviewMapper reviewMapper;

    @Override
    public Page<Review> listReviews(long page, long size, Long userId) {
        LambdaQueryWrapper<Review> wrapper = new LambdaQueryWrapper<Review>()
                .eq(Review::getDeleted, 0)
                .eq(userId != null, Review::getToId, userId)
                .orderByDesc(Review::getCreatedAt);
        return reviewMapper.selectPage(new Page<>(page, size), wrapper);
    }

    @Override
    public Review create(Review review) {
        review.setId(null);
        review.setDeleted(0);
        LocalDateTime now = LocalDateTime.now();
        review.setCreatedAt(now);
        review.setUpdatedAt(now);
        reviewMapper.insert(review);
        return review;
    }
}
