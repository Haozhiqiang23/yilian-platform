package com.yilian.madp.admin.log.controller;

import com.yilian.madp.admin.log.dto.AdminLogListQuery;
import com.yilian.madp.admin.log.service.AdminLogService;
import com.yilian.madp.admin.log.vo.AdminLogListItemResponse;
import com.yilian.madp.admin.log.vo.AdminLogDetailResponse;
import com.yilian.madp.common.PageResult;
import com.yilian.madp.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 管理员操作日志只读查询接口。 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admin/logs")
public class AdminLogController {

    private final AdminLogService adminLogService;


//    把请求参数自动封装到一个 Java 对象里  @ModelAttribute
    @GetMapping
    public Result<PageResult<AdminLogListItemResponse>> getLogs(
            @Valid @ModelAttribute AdminLogListQuery query
    ) {
        return Result.success("查询操作日志成功", adminLogService.getLogs(query));
    }

    @GetMapping("/{id}")
    public Result<AdminLogDetailResponse> getLogDetail(@PathVariable Long id) {
        return Result.success("查询操作日志详情成功", adminLogService.getLogDetail(id));
    }
}
