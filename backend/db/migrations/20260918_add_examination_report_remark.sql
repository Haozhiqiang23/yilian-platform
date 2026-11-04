-- 检查结果录入接口需要将检查结果和医生备注分开保存。
ALTER TABLE examination_report
    ADD COLUMN remark TEXT NULL AFTER result;
