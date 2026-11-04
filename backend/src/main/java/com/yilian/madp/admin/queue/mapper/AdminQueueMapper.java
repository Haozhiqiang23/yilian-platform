package com.yilian.madp.admin.queue.mapper;

import com.yilian.madp.admin.queue.dto.AdminQueueListQuery;
import com.yilian.madp.admin.queue.vo.AdminQueueListItemResponse;
import com.yilian.madp.admin.queue.vo.QueueAdjustLockResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.time.LocalDate;

/** 管理员队列查询数据访问层。 */
@Mapper
public interface AdminQueueMapper {

    int countQueues(@Param("query") AdminQueueListQuery query);

    List<AdminQueueListItemResponse> selectQueues(
            @Param("query") AdminQueueListQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );

    QueueAdjustLockResponse selectQueueForUpdate(@Param("queueId") Long queueId);

    List<Long> selectWaitingQueueIdsForUpdate(
            @Param("doctorId") Long doctorId,
            @Param("visitDate") LocalDate visitDate,
            @Param("timePeriod") String timePeriod
    );

    int updateSortNo(
            @Param("queueId") Long queueId,
            @Param("sortNo") int sortNo
    );

    int insertAdjustLog(
            @Param("adminId") Long adminId,
            @Param("queueId") Long queueId,
            @Param("oldPosition") int oldPosition,
            @Param("newPosition") int newPosition,
            @Param("reason") String reason
    );
}
