package com.electroplate.controller;

import com.electroplate.common.ApiResponse;
import com.electroplate.dto.LogQuery;
import com.electroplate.service.RecipeLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志接口：I7 日志分页查询
 */
@RestController
@RequestMapping("/api/recipe-logs")
@RequiredArgsConstructor
public class RecipeLogController {

    private final RecipeLogService recipeLogService;

    /**
     * I7 操作日志查询
     */
    @GetMapping
    public ApiResponse<Object> page(LogQuery query) {
        return ApiResponse.ok(recipeLogService.page(query));
    }
}
