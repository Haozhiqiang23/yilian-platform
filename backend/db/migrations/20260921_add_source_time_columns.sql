-- 号源时间字段迁移（执行前请确认已备份 yilian_diagnosis_v2）
-- 适用数据库：MySQL 8.0

USE yilian_diagnosis_v2;

ALTER TABLE source
    ADD COLUMN start_time TIME NULL COMMENT '出诊开始时间' AFTER visit_date,
    ADD COLUMN end_time TIME NULL COMMENT '出诊结束时间' AFTER start_time;

-- 兼容旧数据：上午 -> 08:00-12:00，下午 -> 14:00-18:00
-- 使用 HEX 避免 Windows 命令行传输中文时出现编码问题。
UPDATE source
SET start_time = CASE
        WHEN HEX(time_period) = 'E4B88AE58D88' THEN '08:00:00'
        WHEN HEX(time_period) = 'E4B88BE58D88' THEN '14:00:00'
        ELSE start_time
    END,
    end_time = CASE
        WHEN HEX(time_period) = 'E4B88AE58D88' THEN '12:00:00'
        WHEN HEX(time_period) = 'E4B88BE58D88' THEN '18:00:00'
        ELSE end_time
    END
WHERE start_time IS NULL OR end_time IS NULL;

-- 验证：以下查询应返回 0
SELECT COUNT(*) AS invalid_rows
FROM source
WHERE start_time IS NULL
   OR end_time IS NULL
   OR start_time >= end_time;

-- 本阶段保留 time_period，待后端和前端完成切换后再删除。
