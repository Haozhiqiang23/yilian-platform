package com.yilian.madp.doctor.controller;

import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import com.yilian.madp.doctor.dto.DoctorListQuery;
import com.yilian.madp.doctor.service.DoctorService;
import com.yilian.madp.doctor.vo.DoctorListItemResponse;
import com.yilian.madp.doctor.vo.DoctorDetailResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 医生公开信息接口层。
 * 当前接口允许任意已登录角色访问，敏感信息不会通过列表 VO 返回。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;

    /**
     * 按科室、医生姓名和出诊日期分页查询启用医生。
     */
    @GetMapping
    public Result<PageResult<DoctorListItemResponse>> getDoctors(
            @Valid @ModelAttribute DoctorListQuery query
    )
    {
        return Result.success(
                "查询医生列表成功",
                doctorService.getDoctorPage(query)
        );
    }

    /**
     * 查询指定医生的公开详情。
     */
    @GetMapping("/{doctorId}")
    public Result<DoctorDetailResponse> getDoctorDetail(
            @PathVariable Long doctorId
    ) {
        return Result.success(
                "查询医生详情成功",
                doctorService.getDoctorDetail(doctorId)
        );
    }
}
