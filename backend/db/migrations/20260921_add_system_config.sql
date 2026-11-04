-- 管理员系统配置：平台当前只有一套生效参数，因此使用固定主键 id=1。
CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT NOT NULL COMMENT '配置主键，平台级配置固定为1',
    sign_in_ahead_minutes INT NOT NULL DEFAULT 30 COMMENT '允许提前签到的分钟数',
    queue_poll_seconds INT NOT NULL DEFAULT 15 COMMENT '前端刷新排队状态的建议秒数',
    appointment_cancel_hours INT NOT NULL DEFAULT 2 COMMENT '就诊前允许主动取消的最少小时数',
    refund_enabled TINYINT(1) NOT NULL DEFAULT 1 COMMENT '患者退款入口：0关闭，1启用',
    updated_by BIGINT DEFAULT NULL COMMENT '最后修改配置的管理员用户ID',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最后更新时间',
    PRIMARY KEY (id),
    CONSTRAINT fk_system_config_updated_by FOREIGN KEY (updated_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='平台业务参数配置表';

INSERT INTO system_config (id)
VALUES (1)
ON DUPLICATE KEY UPDATE id = VALUES(id);
