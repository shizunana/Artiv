package com.artiv.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.OrderDetailResponse;
import com.artiv.dto.OrderReasonRequest;
import com.artiv.dto.OrderStageReviewRequest;
import com.artiv.dto.OrderStageSubmitRequest;
import com.artiv.entity.OrderInfo;
import com.artiv.security.UserContext;
import com.artiv.service.OrderService;
import com.artiv.service.DisputeService;
import com.artiv.dto.admin.DisputeCreateRequest;
import com.artiv.entity.OrderDispute;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final DisputeService disputeService;

    @GetMapping
    public Result<Page<OrderInfo>> list(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String source,
                                        @RequestParam(required = false) String statusGroup,
                                        @RequestParam(required = false) String participantRole,
                                        @RequestParam(required = false) Long projectId) {
        return Result.ok(orderService.listMine(page, size, UserContext.getUserId(), source, statusGroup,
                participantRole, projectId));
    }

    @GetMapping("/{id}")
    public Result<OrderDetailResponse> detail(@PathVariable Long id) {
        return Result.ok(orderService.getDetail(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/accept")
    public Result<OrderInfo> accept(@PathVariable Long id) {
        return Result.ok(orderService.accept(id, UserContext.getUserId()));
    }

    @PostMapping("/{id}/reject")
    public Result<OrderInfo> reject(@PathVariable Long id, @RequestBody(required = false) OrderReasonRequest request) {
        return Result.ok(orderService.reject(id, UserContext.getUserId(), request == null ? null : request.getReason()));
    }

    @PostMapping("/{id}/cancel")
    public Result<OrderInfo> cancel(@PathVariable Long id, @RequestBody(required = false) OrderReasonRequest request) {
        return Result.ok(orderService.cancel(id, UserContext.getUserId(), request == null ? null : request.getReason()));
    }

    @PostMapping("/{id}/after-sale")
    public Result<OrderDispute> afterSale(@PathVariable Long id, @RequestBody OrderReasonRequest request) {
        DisputeCreateRequest disputeRequest = new DisputeCreateRequest();
        disputeRequest.setReason(request.getReason());
        return Result.ok(disputeService.create(id, UserContext.getUserId(), disputeRequest));
    }

    @PostMapping("/{id}/stages")
    public Result<OrderInfo> submitStage(@PathVariable Long id, @RequestBody OrderStageSubmitRequest request) {
        return Result.ok(orderService.submitStage(id, UserContext.getUserId(), request));
    }

    @PutMapping("/{id}/stages/current")
    public Result<OrderInfo> reviewStage(@PathVariable Long id, @RequestBody OrderStageReviewRequest request) {
        return Result.ok(orderService.reviewStage(id, UserContext.getUserId(), request));
    }
}
