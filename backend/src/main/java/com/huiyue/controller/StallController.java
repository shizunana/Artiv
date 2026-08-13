package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.dto.StallRequest;
import com.huiyue.dto.StallLimitedUpdateRequest;
import com.huiyue.entity.OrderInfo;
import com.huiyue.entity.Stall;
import com.huiyue.security.UserContext;
import com.huiyue.service.OrderService;
import com.huiyue.service.StallService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stalls")
@RequiredArgsConstructor
public class StallController {

    private final StallService stallService;
    private final OrderService orderService;

    @GetMapping
    public Result<Page<Stall>> list(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "12") long size,
                                    @RequestParam(required = false) String category,
                                    @RequestParam(required = false) String keyword,
                                    @RequestParam(required = false) String sort,
                                    @RequestParam(required = false) String subject,
                                    @RequestParam(required = false) java.math.BigDecimal priceMin,
                                    @RequestParam(required = false) java.math.BigDecimal priceMax,
                                    @RequestParam(required = false) String deliverTime) {
        return Result.ok(stallService.listStalls(page, size, category, keyword, sort,
                subject, priceMin, priceMax, deliverTime));
    }

    @GetMapping("/mine")
    public Result<Page<Stall>> mine(@RequestParam(defaultValue = "1") long page,
                                    @RequestParam(defaultValue = "12") long size) {
        return Result.ok(stallService.listMine(page, size, UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<Stall> detail(@PathVariable Long id) {
        return Result.ok(stallService.getById(id, UserContext.getUserId()));
    }

    @PostMapping
    public Result<Stall> create(@Valid @RequestBody StallRequest request) {
        Stall stall = new Stall();
        BeanUtils.copyProperties(request, stall);
        return Result.ok(stallService.create(stall, UserContext.getUserId()));
    }

    @PutMapping("/{id}")
    public Result<Stall> update(@PathVariable Long id,
                                @Valid @RequestBody StallLimitedUpdateRequest request) {
        return Result.ok(stallService.updateLimited(id, request, UserContext.getUserId()));
    }

    @PutMapping("/{id}/status")
    public Result<Stall> changeStatus(@PathVariable Long id, @RequestParam Integer status) {
        return Result.ok(stallService.changeStatus(id, status, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        stallService.delete(id, UserContext.getUserId());
        return Result.ok();
    }

    /** 立即下单：生成订单（金额取橱窗价格） */
    @PostMapping("/{id}/purchase")
    public Result<OrderInfo> purchase(@PathVariable Long id) {
        return Result.ok(orderService.createStallPurchase(id, UserContext.getUserId()));
    }
}
