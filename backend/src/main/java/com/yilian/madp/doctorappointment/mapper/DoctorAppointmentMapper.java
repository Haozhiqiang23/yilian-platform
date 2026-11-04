package com.yilian.madp.doctorappointment.mapper;

import com.yilian.madp.doctorappointment.vo.DoctorAppointmentLockResponse;
import com.yilian.madp.doctorappointment.vo.DoctorQueueLockResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** 医生接诊状态流转的数据访问层。 */
@Mapper
public interface DoctorAppointmentMapper {

    DoctorAppointmentLockResponse selectOwnedAppointmentForUpdate(
            @Param("userId") Long userId,
            @Param("appointmentId") Long appointmentId
    );

    DoctorQueueLockResponse selectQueueForUpdate(
            @Param("appointmentId") Long appointmentId
    );

    int markAppointmentInConsultation(@Param("appointmentId") Long appointmentId);

    int markQueueInConsultation(@Param("queueId") Long queueId);

    int markAppointmentCompleted(@Param("appointmentId") Long appointmentId);

    int markQueueCompleted(@Param("queueId") Long queueId);
}
