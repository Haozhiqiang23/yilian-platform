package com.yilian.madp.refund.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.refund.entity.Refund;
import org.apache.ibatis.annotations.Mapper;

/**
 * 退款申请数据访问层。
 */
@Mapper
public interface RefundMapper extends BaseMapper<Refund> {
}
