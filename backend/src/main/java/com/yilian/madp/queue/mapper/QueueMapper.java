package com.yilian.madp.queue.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.queue.entity.QueueRecord;
import com.yilian.madp.queue.vo.QueueStatusResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 排队记录数据访问层。 */
@Mapper
public interface QueueMapper extends BaseMapper<QueueRecord> {

    /** 查询当前患者自己的排队详情。 */
    QueueStatusResponse selectOwnedQueue(
            @Param("patientId") Long patientId,
            @Param("appointmentId") Long appointmentId
    );

    /** 按 sort_no 统计同一医生、日期和时段中排在前方的等待患者数。 */
    int countWaitingAhead(@Param("queueId") Long queueId);
}
