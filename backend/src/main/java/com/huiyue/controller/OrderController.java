package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.entity.OrderInfo;
import com.huiyue.security.UserContext;
import com.huiyue.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
                                        @RequestParam(defaultValue = "10") long size) {
        return Result.ok(orderService.listMine(page, size, UserContext.getUserId()));
    }

    @GetMapping("/{id}")
    public Result<OrderInfo> detail(@PathVariable Long id) {
        return Result.ok(orderService.getById(id));
    }

    @PostMapping
    public Result<OrderInfo> create(@RequestBody OrderInfo order) {
        return Result.ok(orderService.create(order));
    }
}
