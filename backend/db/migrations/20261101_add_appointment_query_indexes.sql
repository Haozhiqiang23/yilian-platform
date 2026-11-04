-- 【预约查询优化】先确认连接目标库，手动执行；不在应用启动时自动执行。
-- 已有同名索引时停止核对，不能重复执行；大表DDL需评估磁盘、写放大和元数据锁。
-- 全部状态与指定状态是两个排序入口，不能只用patient_id,status,...覆盖全部状态排序。
ALTER TABLE appointment
    ADD INDEX idx_appointment_patient_created
        (patient_id, create_time DESC, id DESC),
    ADD INDEX idx_appointment_patient_status_created
        (patient_id, status, create_time DESC, id DESC);

ANALYZE TABLE appointment;
-- 保留原单列索引，不未经写入/其他查询评估就删除。
