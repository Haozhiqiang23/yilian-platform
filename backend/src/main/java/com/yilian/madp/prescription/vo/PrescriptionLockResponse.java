package com.yilian.madp.prescription.vo;

/** 编辑时加锁读取的最小处方信息。 */
public record PrescriptionLockResponse(
        Long prescriptionId,
        String status
) {
}
