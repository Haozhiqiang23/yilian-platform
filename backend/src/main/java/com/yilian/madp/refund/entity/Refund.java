package com.yilian.madp.refund.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 退款申请实体，与 refund 表对应。
 */
@Data
@TableName("refund")
public class Refund {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long paymentId;

    private BigDecimal amount;

    private String status;

    private String reason;

    private String note;

    private LocalDateTime createTime;

    private LocalDateTime processTime;
}
