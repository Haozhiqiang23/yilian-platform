package com.yilian.madp.message.service;

import com.yilian.madp.common.BusinessException;
import com.yilian.madp.common.ErrorCode;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.message.dto.MessageListQuery;
import com.yilian.madp.message.mapper.MessageMapper;
import com.yilian.madp.message.vo.MessageResponse;
import com.yilian.madp.message.vo.ReadAllResponse;
import com.yilian.madp.message.vo.UnreadCountResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

/** 患者站内消息业务层。 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageMapper messageMapper;

    public PageResult<MessageResponse> getMyMessages(
            Long userId,
            MessageListQuery query
    ) {
        normalizeQuery(query);
        int total = messageMapper.countMine(userId, query);
        int offset = (query.getPage() - 1) * query.getSize();
        List<MessageResponse> records = messageMapper.selectMine(
                userId,
                query,
                offset,
                query.getSize()
        );
        return new PageResult<>(records, total);
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        return new UnreadCountResponse(messageMapper.countUnread(userId));
    }

    public MessageResponse getDetail(Long userId, Long messageId) {
        MessageResponse message = messageMapper.selectOwnedDetail(userId, messageId);
        if (message == null) {
            throw new BusinessException(ErrorCode.MESSAGE_NOT_FOUND);
        }
        return message;
    }

    /**
     * 先校验消息归属，再执行带 is_read=0 条件的更新。
     * 因此不存在或越权返回 404，重复标记已读仍返回成功。
     */
    @Transactional
    public MessageResponse markRead(Long userId, Long messageId) {
        getDetail(userId, messageId);
        messageMapper.markOwnedRead(userId, messageId);
        return getDetail(userId, messageId);
    }

    @Transactional
    public ReadAllResponse markAllRead(Long userId) {
        return new ReadAllResponse(messageMapper.markAllOwnedRead(userId));
    }

    private void normalizeQuery(MessageListQuery query) {
        if (query.getType() != null) {
            String type = query.getType().trim();
            query.setType(type.isEmpty() ? null : type.toUpperCase(Locale.ROOT));
        }
    }
}
