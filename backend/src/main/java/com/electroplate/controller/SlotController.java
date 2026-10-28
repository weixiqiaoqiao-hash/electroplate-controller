package com.electroplate.controller;

import com.electroplate.common.ApiResponse;
import com.electroplate.dto.SlotQuery;
import com.electroplate.dto.SlotSaveRequest;
import com.electroplate.dto.SlotTimeBatchRequest;
import com.electroplate.dto.SlotVO;
import com.electroplate.dto.SwitchRequest;
import com.electroplate.service.SlotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 槽位接口：J1 列表、J2 新增、J3 修改、J4 删除、J5 批量保存时间、J6 开关切换
 */
@RestController
@RequestMapping("/api/slots")
@RequiredArgsConstructor
public class SlotController {

    private final SlotService slotService;

    /**
     * J1 槽位列表（全量，按工序序号升序）
     */
    @GetMapping
    public ApiResponse<List<SlotVO>> list(SlotQuery query) {
        return ApiResponse.ok(slotService.list(query));
    }

    /**
     * J2 新增槽位，成功返回 201
     */
    @PostMapping
    public ResponseEntity<ApiResponse<SlotVO>> create(@Valid @RequestBody SlotSaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(slotService.create(request)));
    }

    /**
     * J3 修改槽位配置
     */
    @PutMapping("/{id}")
    public ApiResponse<SlotVO> update(@PathVariable("id") Long id,
                                      @Valid @RequestBody SlotSaveRequest request) {
        return ApiResponse.ok(slotService.update(id, request));
    }

    /**
     * J4 删除槽位（物理删除，名称与序号即释放），成功返回 204
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        slotService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * J5 批量保存时间（时间设定模式的统一保存）
     */
    @PutMapping("/times")
    public ApiResponse<Void> updateTimes(@Valid @RequestBody SlotTimeBatchRequest request) {
        slotService.updateTimes(request);
        return ApiResponse.ok(null);
    }

    /**
     * J6 镀槽开关即时切换
     */
    @PutMapping("/{id}/switch")
    public ApiResponse<SlotVO> switchSlot(@PathVariable("id") Long id,
                                          @Valid @RequestBody SwitchRequest request) {
        return ApiResponse.ok(slotService.switchSlot(id, request.getSwitchOn()));
    }
}
