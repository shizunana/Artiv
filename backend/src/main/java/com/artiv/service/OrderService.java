package com.artiv.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.dto.OrderDetailResponse;
import com.artiv.dto.OrderStageReviewRequest;
import com.artiv.dto.OrderStageSubmitRequest;
import com.artiv.entity.OrderInfo;

public interface OrderService {

    Page<OrderInfo> listMine(long page, long size, Long userId, String source, String statusGroup, String participantRole);

    OrderDetailResponse getDetail(Long id, Long userId);

    OrderInfo createStallPurchase(Long stallId, Long employerId);

    OrderInfo createProjectOrder(Long projectId, Long applicationId, Long employerId);

    OrderInfo accept(Long id, Long userId);

    OrderInfo reject(Long id, Long userId, String reason);

    OrderInfo cancel(Long id, Long userId, String reason);

    OrderInfo requestAfterSale(Long id, Long userId, String reason);

    OrderInfo submitStage(Long id, Long userId, OrderStageSubmitRequest request);

    OrderInfo reviewStage(Long id, Long userId, OrderStageReviewRequest request);
}
