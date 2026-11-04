-- 精简系统配置：暂不提供需要定时任务和全局拦截支持的复杂配置。
ALTER TABLE system_config
    DROP COLUMN payment_timeout_minutes,
    DROP COLUMN maintenance_mode;
