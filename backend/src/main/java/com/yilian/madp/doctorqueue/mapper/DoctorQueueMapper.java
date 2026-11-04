package com.yilian.madp.doctorqueue.mapper;

import com.yilian.madp.doctorqueue.dto.DoctorQueueQuery;
import com.yilian.madp.doctorqueue.vo.DoctorQueueItemResponse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/** 医生患者队列数据访问层。 */
@Mapper
public interface DoctorQueueMapper {

    int countMine(
            @Param("doctorId") Long doctorId,
            @Param("visitDate") LocalDate visitDate,
            @Param("query") DoctorQueueQuery query
    );

    List<DoctorQueueItemResponse> selectMine(
            @Param("doctorId") Long doctorId,
            @Param("visitDate") LocalDate visitDate,
            @Param("query") DoctorQueueQuery query,
            @Param("offset") int offset,
            @Param("limit") int limit
    );
}
