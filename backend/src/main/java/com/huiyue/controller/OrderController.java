package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.dto.OrderDetailResponse;
import com.huiyue.dto.OrderReasonRequest;
import com.huiyue.dto.OrderStageReviewRequest;
import com.huiyue.dto.OrderStageSubmitRequest;
import com.huiyue.entity.OrderInfo;
import com.huiyue.security.UserContext;
import com.huiyue.service.OrderService;
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

    @GetMapping
    public Result<Page<OrderInfo>> list(@RequestParam(defaultValue = "1") long page,
                                        @RequestParam(defaultValue = "10") long size,
                                        @RequestParam(required = false) String source,
                                        @RequestParam(required = false) String statusGroup,
                                        @RequestParam(required = false) String participantRole) {
        return Result.ok(orderService.listMine(page, size, UserContext.getUserId(), source, statusGroup, participantRole));
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
    public Result<OrderInfo> afterSale(@PathVariable Long id, @RequestBody OrderReasonRequest request) {
        return Result.ok(orderService.requestAfterSale(id, UserContext.getUserId(), request.getReason()));
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
