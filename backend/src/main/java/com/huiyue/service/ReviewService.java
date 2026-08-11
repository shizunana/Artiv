package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.entity.Review;

public interface ReviewService {

    Page<Review> listReviews(long page, long size, Long userId);

    Review create(Review review);
}
