-- 号源挂号费迁移
-- 主任医师 50 元，副主任医师 30 元，主治医师及其他 20 元

USE yilian_diagnosis_v2;

ALTER TABLE source
    ADD COLUMN fee DECIMAL(10,2) NOT NULL DEFAULT 20.00 COMMENT '挂号费用（元）' AFTER end_time;

UPDATE source s
INNER JOIN doctor d ON d.id = s.doctor_id
SET s.fee = CASE
    WHEN d.title = '主任医师' THEN 50.00
    WHEN d.title = '副主任医师' THEN 30.00
    ELSE 20.00
END;

-- 验证费用不能为空且必须大于 0
SELECT COUNT(*) AS invalid_fee_rows
FROM source
WHERE fee IS NULL OR fee <= 0;
