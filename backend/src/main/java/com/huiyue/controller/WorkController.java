package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.entity.Work;
import com.huiyue.security.UserContext;
import com.huiyue.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/works")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    @GetMapping
    public Result<Page<Work>> list(@RequestParam(defaultValue = "1") long page,
                                   @RequestParam(defaultValue = "12") long size,
                                   @RequestParam(required = false) String category) {
        return Result.ok(workService.listWorks(page, size, category));
    }

    @PostMapping
    public Result<Work> create(@RequestBody Work work) {
        return Result.ok(workService.create(work, UserContext.getUserId()));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        workService.delete(id, UserContext.getUserId());
        return Result.ok();
    }
}
