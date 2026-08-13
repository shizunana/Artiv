package com.huiyue.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.dto.OrderDetailResponse;
import com.huiyue.dto.OrderStageReviewRequest;
import com.huiyue.dto.OrderStageSubmitRequest;
import com.huiyue.entity.OrderInfo;

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
