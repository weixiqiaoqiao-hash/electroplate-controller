package com.electroplate.controller;

import com.electroplate.common.ApiResponse;
import com.electroplate.dto.DictItemRequest;
import com.electroplate.dto.DictItemVO;
import com.electroplate.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 字典接口：I8 字典列表、I9 新增字典项、I10 删除字典项
 */
@RestController
@RequestMapping("/api/dicts")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /**
     * I8 字典列表（CATEGORY/SPEC）
     */
    @GetMapping
    public ApiResponse<Object> list(@RequestParam("type") String type) {
        return ApiResponse.ok(dictService.list(type));
    }

    /**
     * I9 新增字典项，成功返回 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<DictItemVO>> create(@Valid @RequestBody DictItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(dictService.create(request)));
    }

    /**
     * I10 删除字典项，成功返回 204 无响应体
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        dictService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
