package com.yilian.madp.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.message.dto.MessageListQuery;
import com.yilian.madp.message.entity.Message;
import com.yilian.madp.message.vo.MessageResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/** 站内消息数据访问层。 */
@Mapper
public interface MessageMapper extends BaseMapper<Message> {

    int countMine(
            @Param("userId") Long userId,
            @Param("query") MessageListQuery query
    );

    List<MessageResponse> selectMine(
            @Param("userId") Long userId,
            @Param("query") MessageListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    MessageResponse selectOwnedDetail(
            @Param("userId") Long userId,
            @Param("messageId") Long messageId
    );

    int countUnread(@Param("userId") Long userId);

    int markOwnedRead(
            @Param("userId") Long userId,
            @Param("messageId") Long messageId
    );

    int markAllOwnedRead(@Param("userId") Long userId);
}
