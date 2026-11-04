package com.yilian.madp.signin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 签到记录实体，与 sign_in 表对应。 */
@Data
@TableName("sign_in")
public class SignIn {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long appointmentId;

    private LocalDateTime signTime;
}
