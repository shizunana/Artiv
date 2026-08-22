package com.artiv.service;

import com.artiv.dto.OrderReviewContext;
import com.artiv.dto.ReviewAppealRequest;
import com.artiv.dto.ReviewListItem;
import com.artiv.dto.ReviewRequest;
import com.artiv.dto.ReviewStatistics;
import com.artiv.dto.admin.ReviewAppealDetail;
import com.artiv.entity.OrderInfo;
import com.artiv.entity.ReviewAppeal;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public interface ReviewService {

    Page<ReviewListItem> listPublic(long page, long size, Long userId, String role, Long viewerId);

    ReviewListItem create(Long orderId, Long userId, ReviewRequest request);

    OrderReviewContext getOrderReviewContext(OrderInfo order, Long userId);

    ReviewStatistics getStatistics(Long userId, String role);

    Map<Long, ReviewStatistics> getStatistics(List<Long> userIds, String role);

    ReviewAppeal submitAppeal(Long reviewId, Long userId, ReviewAppealRequest request);

    ReviewAppeal getOwnAppeal(Long reviewId, Long userId);

    Page<ReviewAppealDetail> listAppeals(long page, long size, Integer status, String keyword,
                                         LocalDate startDate, LocalDate endDate);

    ReviewAppealDetail getAppeal(Long appealId);

    ReviewAppealDetail resolveAppeal(Long appealId, Long adminId, boolean approve, String reason);

    long countPendingAppeals();
}
