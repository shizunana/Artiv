package com.huiyue.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.entity.Project;
import com.huiyue.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @GetMapping
    public Result<Page<Project>> list(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size,
                                      @RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return Result.ok(projectService.listProjects(page, size, keyword, status));
    }
}
