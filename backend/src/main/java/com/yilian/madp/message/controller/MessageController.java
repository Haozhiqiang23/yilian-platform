package com.yilian.madp.message.controller;

import com.yilian.madp.auth.service.CurrentUserService;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.message.dto.MessageListQuery;
import com.yilian.madp.message.service.MessageService;
import com.yilian.madp.message.vo.MessageResponse;
import com.yilian.madp.message.vo.ReadAllResponse;
import com.yilian.madp.message.vo.UnreadCountResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 患者站内消息接口层。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/messages")
public class MessageController {

    private final MessageService messageService;
    private final CurrentUserService currentUserService;

    @GetMapping
    public Result<PageResult<MessageResponse>> getMyMessages(
            @Valid @ModelAttribute MessageListQuery query
    ) {
        return Result.success(
                "查询成功",
                messageService.getMyMessages(currentUserId(), query)
        );
    }

    @GetMapping("/unread-count")
    public Result<UnreadCountResponse> getUnreadCount() {
        return Result.success(
                "查询成功",
                messageService.getUnreadCount(currentUserId())
        );
    }

    @GetMapping("/{id}")
    public Result<MessageResponse> getDetail(@PathVariable Long id) {
        return Result.success(
                "查询成功",
                messageService.getDetail(currentUserId(), id)
        );
    }

    @PutMapping("/{id}/read")
    public Result<MessageResponse> markRead(@PathVariable Long id) {
        return Result.success(
                "已标记为已读",
                messageService.markRead(currentUserId(), id)
        );
    }

    @PutMapping("/read-all")
    public Result<ReadAllResponse> markAllRead() {
        return Result.success(
                "全部消息已读",
                messageService.markAllRead(currentUserId())
        );
    }

    private Long currentUserId() {
        return currentUserService.getCurrentUser().getUserId();
    }
}
