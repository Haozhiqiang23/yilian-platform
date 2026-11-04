package com.yilian.madp.signin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yilian.madp.signin.entity.SignIn;
import com.yilian.madp.signin.vo.SignInAppointmentLockResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

/** 签到数据访问层；加锁和排队号计算 SQL 放在 XML 中。 */
@Mapper
public interface SignInMapper extends BaseMapper<SignIn> {

    SignInAppointmentLockResponse selectOwnedAppointmentForUpdate(
            @Param("patientId") Long patientId,
            @Param("appointmentId") Long appointmentId
    );

    int countByAppointmentId(@Param("appointmentId") Long appointmentId);

    /** 锁定医生行，使同一医生的并发排队号按顺序生成。 */
    Long lockDoctor(@Param("doctorId") Long doctorId);

    int selectMaxQueueSequence(
            @Param("doctorId") Long doctorId,
            @Param("visitDate") LocalDate visitDate
    );

    int markAppointmentSignedIn(
            @Param("appointmentId") Long appointmentId
    );


}
