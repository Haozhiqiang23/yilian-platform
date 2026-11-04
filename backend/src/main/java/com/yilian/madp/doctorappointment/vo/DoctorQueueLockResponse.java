package com.yilian.madp.doctorappointment.vo;

/** 开始接诊时锁定读取的队列状态。 */
public record DoctorQueueLockResponse(
        Long queueId,
        String status
) {
}
