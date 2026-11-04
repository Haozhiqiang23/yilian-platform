-- 合成演示库。已存在则停止，不含DROP。默认密码123456仅供本机演示。
CREATE DATABASE yilian_demo CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
USE yilian_demo;
CREATE TABLE sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '系统用户主键',
    username VARCHAR(50) NOT NULL COMMENT '登录账号：管理员/医生账号或患者手机号',
    password VARCHAR(255) NOT NULL COMMENT 'BCrypt 加密后的登录密码',
    role VARCHAR(20) NOT NULL COMMENT '用户角色：ADMIN、DOCTOR、PATIENT',
    status VARCHAR(20) NOT NULL DEFAULT '正常' COMMENT '账号状态：正常、禁用',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '账号创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统用户表：保存登录认证和角色信息';


CREATE TABLE department (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '科室主键',
    name VARCHAR(50) NOT NULL COMMENT '科室名称',
    PRIMARY KEY (id),
    UNIQUE KEY uk_department_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='科室信息表';


CREATE TABLE doctor (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '医生主键',
    user_id BIGINT NOT NULL COMMENT '关联的系统用户ID',
    department_id BIGINT NOT NULL COMMENT '所属科室ID',
    name VARCHAR(50) NOT NULL COMMENT '医生姓名',
    title VARCHAR(50) DEFAULT NULL COMMENT '医生职称',
    introduction TEXT COMMENT '医生简介和擅长方向',
    status VARCHAR(20) NOT NULL DEFAULT '启用' COMMENT '医生业务状态：启用、停用',
    disable_reason VARCHAR(100) DEFAULT NULL COMMENT '当前禁用原因，恢复后清空',
    disable_time DATETIME DEFAULT NULL COMMENT '当前禁用时间，恢复后清空',
    disabled_by BIGINT DEFAULT NULL COMMENT '执行当前禁用操作的管理员用户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_user_id (user_id),
    KEY idx_doctor_department_id (department_id),
    KEY idx_doctor_disabled_by (disabled_by),
    CONSTRAINT fk_doctor_department FOREIGN KEY (department_id) REFERENCES department (id),
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_doctor_disabled_by FOREIGN KEY (disabled_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='医生资料表';


CREATE TABLE patient (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '患者主键',
    user_id BIGINT NOT NULL COMMENT '关联的系统用户ID',
    name VARCHAR(50) NOT NULL COMMENT '患者真实姓名',
    id_card VARCHAR(18) NOT NULL COMMENT '患者身份证号',
    phone VARCHAR(20) NOT NULL COMMENT '患者联系电话',
    address VARCHAR(255) DEFAULT NULL COMMENT '患者联系地址',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '患者资料创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_patient_user_id (user_id),
    UNIQUE KEY uk_patient_id_card (id_card),
    UNIQUE KEY uk_patient_phone (phone),
    CONSTRAINT fk_patient_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='患者实名资料表';


CREATE TABLE source (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '号源主键',
    doctor_id BIGINT NOT NULL COMMENT '出诊医生ID',
    visit_date DATE NOT NULL COMMENT '就诊日期',
    time_period VARCHAR(50) NOT NULL COMMENT '就诊时段：上午、下午',
    start_time TIME NOT NULL COMMENT '出诊开始时间',
    end_time TIME NOT NULL COMMENT '出诊结束时间',
    fee DECIMAL(10,2) NOT NULL COMMENT '挂号费用',
    total_count INT NOT NULL COMMENT '号源总数量',
    remain_count INT NOT NULL COMMENT '剩余可预约数量',
    status VARCHAR(20) NOT NULL DEFAULT '正常' COMMENT '号源状态：正常、停诊',
    suspension_reason VARCHAR(100) DEFAULT NULL COMMENT '停诊原因',
    suspension_time DATETIME DEFAULT NULL COMMENT '停诊操作时间',
    suspended_by BIGINT DEFAULT NULL COMMENT '执行停诊的管理员用户ID',
    PRIMARY KEY (id),
    UNIQUE KEY uk_doctor_time (doctor_id, visit_date, time_period),
    KEY idx_source_status (status),
    KEY idx_source_suspended_by (suspended_by),
    CONSTRAINT fk_source_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_source_suspended_by FOREIGN KEY (suspended_by) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='医生排班号源表';


CREATE TABLE appointment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '预约主键',
    patient_id BIGINT NOT NULL COMMENT '预约患者ID',
    doctor_id BIGINT NOT NULL COMMENT '预约医生ID',
    source_id BIGINT NOT NULL COMMENT '预约号源ID',
    status VARCHAR(30) NOT NULL COMMENT '预约状态：待支付、已支付、已签到、接诊中、已完成、取消、退款中',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '预约创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '预约最后更新时间',
    PRIMARY KEY (id),
    KEY idx_appointment_patient (patient_id),
    KEY idx_appointment_doctor (doctor_id),
    KEY fk_appointment_source (source_id),
    CONSTRAINT fk_appointment_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_appointment_patient FOREIGN KEY (patient_id) REFERENCES patient (id),
    CONSTRAINT fk_appointment_source FOREIGN KEY (source_id) REFERENCES source (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='患者预约记录表';


CREATE TABLE payment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付订单主键',
    appointment_id BIGINT NOT NULL COMMENT '关联预约ID',
    order_no VARCHAR(64) NOT NULL COMMENT '支付订单号',
    amount DECIMAL(10,2) NOT NULL COMMENT '支付金额',
    status VARCHAR(20) NOT NULL COMMENT '支付状态：待支付、支付成功、已关闭',
    pay_time DATETIME DEFAULT NULL COMMENT '实际支付时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_payment_appointment_id (appointment_id),
    UNIQUE KEY uk_payment_order_no (order_no),
    CONSTRAINT fk_payment_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='预约支付订单表';


CREATE TABLE sign_in (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '签到记录主键',
    appointment_id BIGINT NOT NULL COMMENT '关联预约ID',
    sign_time DATETIME NOT NULL COMMENT '患者签到时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sign_in_appointment_id (appointment_id),
    CONSTRAINT fk_signin_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='患者签到记录表';


CREATE TABLE queue (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '排队记录主键',
    appointment_id BIGINT NOT NULL COMMENT '关联预约ID',
    queue_no VARCHAR(20) NOT NULL COMMENT '当日排队号码',
    sign_time DATETIME NOT NULL COMMENT '进入队列时间',
    sort_no INT NOT NULL COMMENT '等待队列业务排序值，不改变真实签到时间',
    status VARCHAR(20) NOT NULL COMMENT '队列状态：等待接诊、接诊中、完成',
    PRIMARY KEY (id),
    UNIQUE KEY uk_queue_appointment_id (appointment_id),
    CONSTRAINT fk_queue_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='医生接诊排队表';


CREATE TABLE refund (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '退款申请主键',
    payment_id BIGINT NOT NULL COMMENT '关联支付订单ID',
    amount DECIMAL(10,2) NOT NULL COMMENT '退款金额',
    status VARCHAR(20) NOT NULL COMMENT '退款状态：退款申请、退款成功、退款拒绝',
    reason VARCHAR(255) DEFAULT NULL COMMENT '患者退款原因',
    note VARCHAR(255) DEFAULT NULL COMMENT '管理员处理备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '退款申请时间',
    process_time DATETIME DEFAULT NULL COMMENT '退款处理时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_refund_payment_id (payment_id),
    CONSTRAINT fk_refund_payment FOREIGN KEY (payment_id) REFERENCES payment (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='预约退款申请及处理记录表';


CREATE TABLE medical_record (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '诊疗记录主键',
    appointment_id BIGINT NOT NULL COMMENT '关联预约ID',
    doctor_id BIGINT NOT NULL COMMENT '接诊医生ID',
    patient_id BIGINT NOT NULL COMMENT '就诊患者ID',
    diagnosis TEXT COMMENT '诊断结果',
    treatment TEXT COMMENT '治疗建议',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '诊疗记录创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_medical_record_appointment_id (appointment_id),
    KEY idx_medical_record_doctor_id (doctor_id),
    KEY idx_medical_record_patient_id (patient_id),
    CONSTRAINT fk_record_appointment FOREIGN KEY (appointment_id) REFERENCES appointment (id),
    CONSTRAINT fk_record_doctor FOREIGN KEY (doctor_id) REFERENCES doctor (id),
    CONSTRAINT fk_record_patient FOREIGN KEY (patient_id) REFERENCES patient (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='患者诊疗病历表';


CREATE TABLE prescription (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '处方主键',
    medical_record_id BIGINT NOT NULL COMMENT '关联诊疗记录ID',
    status VARCHAR(20) NOT NULL COMMENT '处方状态：草稿、已提交',
    medicine_info TEXT COMMENT '药品名称、规格和数量',
    usage_info TEXT COMMENT '药品用法用量',
    amount DECIMAL(10,2) DEFAULT NULL COMMENT '处方药品模拟金额',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '处方创建时间',
    PRIMARY KEY (id),
    KEY idx_prescription_medical_record_id (medical_record_id),
    CONSTRAINT fk_prescription_record FOREIGN KEY (medical_record_id) REFERENCES medical_record (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='医生处方表';


CREATE TABLE examination (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '检查申请主键',
    medical_record_id BIGINT NOT NULL COMMENT '关联诊疗记录ID',
    item_name VARCHAR(100) NOT NULL COMMENT '检查项目名称',
    status VARCHAR(20) NOT NULL COMMENT '检查状态：申请、执行中、待医生确认、需补充、完成',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检查申请创建时间',
    PRIMARY KEY (id),
    KEY idx_examination_medical_record_id (medical_record_id),
    CONSTRAINT fk_examination_record FOREIGN KEY (medical_record_id) REFERENCES medical_record (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='医生检查申请表';


CREATE TABLE examination_report (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '检查报告主键',
    examination_id BIGINT NOT NULL COMMENT '关联检查申请ID',
    result TEXT COMMENT '检查结果',
    remark TEXT COMMENT '医生检查备注',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '检查报告生成时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_examination_report_examination_id (examination_id),
    CONSTRAINT fk_report_examination FOREIGN KEY (examination_id) REFERENCES examination (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='检查结果报告表';


CREATE TABLE message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '站内消息主键',
    recipient_user_id BIGINT NOT NULL COMMENT '消息接收用户ID',
    message_type VARCHAR(30) NOT NULL COMMENT '消息类型：APPOINTMENT、PAYMENT、REMINDER',
    title VARCHAR(100) NOT NULL COMMENT '消息标题',
    content TEXT NOT NULL COMMENT '消息正文',
    related_type VARCHAR(30) DEFAULT NULL COMMENT '关联业务类型',
    related_id BIGINT DEFAULT NULL COMMENT '关联业务数据ID',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读：0未读，1已读',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '消息创建时间',
    read_time DATETIME DEFAULT NULL COMMENT '消息阅读时间',
    PRIMARY KEY (id),
    KEY idx_message_recipient_read (recipient_user_id, is_read),
    KEY idx_message_create_time (create_time),
    CONSTRAINT fk_message_recipient FOREIGN KEY (recipient_user_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='用户站内消息表';


CREATE TABLE system_config (
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


CREATE TABLE operation_log (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '操作日志主键',
    operator_id BIGINT DEFAULT NULL COMMENT '操作用户ID',
    operation_type VARCHAR(50) DEFAULT NULL COMMENT '操作类型',
    action VARCHAR(30) DEFAULT NULL COMMENT '结构化动作，如PASS、REJECT、DISABLE、RESTORE',
    target_type VARCHAR(30) DEFAULT NULL COMMENT '结构化操作对象类型，如DOCTOR',
    target_id BIGINT DEFAULT NULL COMMENT '结构化操作对象主键',
    target VARCHAR(100) DEFAULT NULL COMMENT '操作对象',
    reason VARCHAR(100) DEFAULT NULL COMMENT '操作原因或审核原因',
    result VARCHAR(20) DEFAULT NULL COMMENT '操作结果',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_operation_log_operator_id (operator_id),
    KEY idx_operation_log_target (target_type, target_id),
    CONSTRAINT fk_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='系统敏感操作审计日志表';


INSERT INTO sys_user(id,username,password,role) VALUES(1,'demo_admin','$2a$10$NT9T4YiYlR9RBYkuQ7kiVe5dCwDcXigUlTEKrPGgoMfnU2AX/fo/e','ADMIN');
INSERT INTO sys_user(id,username,password,role) VALUES(2,'demo_doctor','$2a$10$NT9T4YiYlR9RBYkuQ7kiVe5dCwDcXigUlTEKrPGgoMfnU2AX/fo/e','DOCTOR');
INSERT INTO sys_user(id,username,password,role) VALUES(3,'demo_patient','$2a$10$NT9T4YiYlR9RBYkuQ7kiVe5dCwDcXigUlTEKrPGgoMfnU2AX/fo/e','PATIENT');
INSERT INTO department(id,name) VALUES(1,'演示科室');
INSERT INTO doctor(id,user_id,department_id,name,title,introduction) VALUES(1,2,1,'演示医生','演示职称','人工合成演示资料');
INSERT INTO patient(id,user_id,name,id_card,phone) VALUES(1,3,'演示患者','990000000000000001','19900000001');
INSERT INTO source(id,doctor_id,visit_date,time_period,start_time,end_time,fee,total_count,remain_count) VALUES(1,1,DATE_ADD(CURDATE(),INTERVAL 7 DAY),'上午','08:00:00','12:00:00',20,20,20);
INSERT INTO system_config(id) VALUES(1);