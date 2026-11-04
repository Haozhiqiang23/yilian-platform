package com.yilian.madp.admin.queue.vo;

/** 调序成功后返回原位置和新位置。 */
public record QueueAdjustResponse(
        Long queueId,
        Integer oldPosition,
        Integer newPosition
) {
}
