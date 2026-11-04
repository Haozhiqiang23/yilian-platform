package com.yilian.madp.source.controller;

import com.yilian.madp.common.Result;
import com.yilian.madp.source.dto.SourceListQuery;
import com.yilian.madp.source.service.SourceService;
import com.yilian.madp.source.vo.SourceResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 号源查询接口，所有已登录角色都可以访问。
 */
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/sources")
public class SourceController {

    private final SourceService sourceService;

    /**
     * 查询指定医生的号源，date 不传时返回该医生全部号源。
     */
    @GetMapping
    public Result<List<SourceResponse>> getSources(
            @Valid @ModelAttribute SourceListQuery query
    ) {
        return Result.success(
                "查询号源成功",
                sourceService.getSources(query)
        );
    }
}
