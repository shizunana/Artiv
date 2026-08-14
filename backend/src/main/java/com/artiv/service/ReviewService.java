package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.entity.Review;

public interface ReviewService {

    Page<Review> listReviews(long page, long size, Long userId);

    Review create(Review review);
}
