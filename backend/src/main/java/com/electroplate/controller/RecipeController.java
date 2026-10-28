package com.electroplate.controller;

import com.electroplate.common.ApiResponse;
import com.electroplate.dto.RecipeQuery;
import com.electroplate.dto.RecipeSaveRequest;
import com.electroplate.dto.RecipeVO;
import com.electroplate.dto.StatusRequest;
import com.electroplate.service.RecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.math.BigDecimal;

/**
 * 配方接口：I1 分页查询、I2 详情、I3 新增、I4 修改、I5 停用/启用、I6 匹配查询
 */
@RestController
@RequestMapping("/api/recipes")
@RequiredArgsConstructor
public class RecipeController {

    private final RecipeService recipeService;

    /**
     * I1 分页查询配方
     */
    @GetMapping
    public ApiResponse<Object> page(RecipeQuery query) {
        return ApiResponse.ok(recipeService.page(query));
    }

    /**
     * I2 配方详情
     */
    @GetMapping("/{id}")
    public ApiResponse<RecipeVO> detail(@PathVariable("id") Long id) {
        return ApiResponse.ok(recipeService.detail(id));
    }

    /**
     * I3 新增配方，成功返回 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<RecipeVO>> create(@Valid @RequestBody RecipeSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(recipeService.create(request)));
    }

    /**
     * I4 修改配方
     */
    @PutMapping("/{id}")
    public ApiResponse<RecipeVO> update(@PathVariable("id") Long id,
                                        @Valid @RequestBody RecipeSaveRequest request) {
        return ApiResponse.ok(recipeService.update(id, request));
    }

    /**
     * I5 停用/启用
     */
    @PutMapping("/{id}/status")
    public ApiResponse<RecipeVO> changeStatus(@PathVariable("id") Long id,
                                              @Valid @RequestBody StatusRequest request) {
        return ApiResponse.ok(recipeService.changeStatus(id, request));
    }

    /**
     * I6 匹配查询（供扫码上料模块调用），四参数全必填；
     * 无命中返回 200 + data=null + 提示"无匹配配方"
     */
    @GetMapping("/match")
    public ApiResponse<RecipeVO> match(@RequestParam("category") String category,
                                       @RequestParam("spec") String spec,
                                       @RequestParam("resistance") BigDecimal resistance,
                                       @RequestParam("qty") Integer qty) {
        RecipeVO matched = recipeService.match(category, spec, resistance, qty);
        if (matched == null) {
            return new ApiResponse<RecipeVO>(0, "无匹配配方", null);
        }
        return ApiResponse.ok(matched);
    }
}
