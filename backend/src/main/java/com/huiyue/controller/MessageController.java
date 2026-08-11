package com.huiyue.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.huiyue.common.Result;
import com.huiyue.entity.Message;
import com.huiyue.mapper.MessageMapper;
import com.huiyue.security.UserContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

/**
 * 消息模块：我的消息 / 已读
 */
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageController {

    private final MessageMapper messageMapper;

    @GetMapping
    public Result<Page<Message>> list(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        Long userId = UserContext.getUserId();
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getToId, userId)
                .orderByDesc(Message::getCreatedAt);
        return Result.ok(messageMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getId, id)
                .eq(Message::getToId, userId)
                .set(Message::getReadAt, LocalDateTime.now()));
        return Result.ok();
    }

    @PutMapping("/read-all")
    public Result<Void> readAll() {
        Long userId = UserContext.getUserId();
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getToId, userId)
                .eq(Message::getDeleted, 0)
                .isNull(Message::getReadAt)
                .set(Message::getReadAt, LocalDateTime.now()));
        return Result.ok();
    }
}
