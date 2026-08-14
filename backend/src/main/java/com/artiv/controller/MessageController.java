package com.artiv.controller;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.artiv.common.Result;
import com.artiv.dto.ConversationSummary;
import com.artiv.dto.PrivateMessageRequest;
import com.artiv.entity.Message;
import com.artiv.mapper.MessageMapper;
import com.artiv.security.UserContext;
import com.artiv.service.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final MessageService messageService;

    @GetMapping
    public Result<Page<Message>> list(@RequestParam(defaultValue = "1") long page,
                                      @RequestParam(defaultValue = "10") long size) {
        Long userId = UserContext.getUserId();
        var wrapper = new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Message>()
                .eq(Message::getDeleted, 0)
                .eq(Message::getToId, userId)
                .orderByDesc(Message::getCreatedAt);
        return Result.ok(messageMapper.selectPage(new Page<>(page, size), wrapper));
    }

    @PostMapping("/private")
    public Result<Message> sendPrivate(@RequestBody PrivateMessageRequest request) {
        return Result.ok(messageService.sendPrivateMessage(UserContext.getUserId(), request));
    }

    @GetMapping("/conversation")
    public Result<Page<Message>> conversation(@RequestParam Long withUserId,
                                               @RequestParam(required = false) Long projectId,
                                               @RequestParam(required = false) Long orderId,
                                               @RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size) {
        return Result.ok(messageService.listConversation(UserContext.getUserId(), withUserId, projectId, orderId, page, size));
    }

    @GetMapping("/conversations")
    public Result<Page<ConversationSummary>> conversations(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "20") long size) {
        return Result.ok(messageService.listConversations(UserContext.getUserId(), page, size));
    }

    @GetMapping("/unread-count")
    public Result<Long> unreadCount() {
        return Result.ok(messageService.countUnread(UserContext.getUserId()));
    }

    @GetMapping("/notifications")
    public Result<Page<Message>> notifications(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long size) {
        return Result.ok(messageService.listNotifications(UserContext.getUserId(), page, size));
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

    @PutMapping("/notifications/read-all")
    public Result<Void> readAllNotifications() {
        Long userId = UserContext.getUserId();
        messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                .eq(Message::getToId, userId)
                .eq(Message::getDeleted, 0)
                .ne(Message::getType, 3)
                .isNull(Message::getReadAt)
                .set(Message::getReadAt, LocalDateTime.now()));
        return Result.ok();
    }
}
