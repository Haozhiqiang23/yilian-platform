package com.yilian.madp.admin.queue.vo;

import java.time.LocalDate;

/** 调序事务中锁定的队列上下文。 */
public record QueueAdjustLockResponse(
        Long queueId,
        Long doctorId,
        LocalDate visitDate,
        String timePeriod,
        String status,
        Integer sortNo
) {
}
