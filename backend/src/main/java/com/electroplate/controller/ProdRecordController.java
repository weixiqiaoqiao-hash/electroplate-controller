package com.electroplate.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.electroplate.common.ApiResponse;
import com.electroplate.dto.ProdRecordCreateRequest;
import com.electroplate.dto.ProdRecordQuery;
import com.electroplate.dto.ProdRecordVO;
import com.electroplate.service.ProdRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.net.URLEncoder;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 生产信息记录接口：K1 分页查询、K2 导出 Excel、K3 新增（扫码上料预留）
 */
@RestController
@RequestMapping("/api/prod-records")
@RequiredArgsConstructor
public class ProdRecordController {

    private final ProdRecordService prodRecordService;

    /**
     * K1 分页查询（默认投料时间倒序）
     */
    @GetMapping
    public ApiResponse<Page<ProdRecordVO>> page(ProdRecordQuery query) {
        return ApiResponse.ok(prodRecordService.page(query));
    }

    /**
     * K2 导出 Excel：参数同 K1（不含分页），文件名 生产记录_导出时刻.xlsx
     */
    @GetMapping("/export")
    public ResponseEntity<ByteArrayResource> export(ProdRecordQuery query) {
        byte[] bytes = prodRecordService.export(query);
        String fileName = "生产记录_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss")) + ".xlsx";
        String encoded;
        try {
            encoded = URLEncoder.encode(fileName, "UTF-8").replaceAll("\\+", "%20");
        } catch (java.io.UnsupportedEncodingException e) {
            encoded = "production-records.xlsx";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentLength(bytes.length)
                .body(new ByteArrayResource(bytes));
    }

    /**
     * K3 新增记录：扫码上料「发送」时调用，本期无页面入口
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProdRecordVO> create(@Valid @RequestBody ProdRecordCreateRequest request) {
        return ApiResponse.ok(prodRecordService.create(request));
    }
}
