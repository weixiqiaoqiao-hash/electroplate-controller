package com.electroplate.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.electroplate.common.BusinessException;
import com.electroplate.common.ErrorCode;
import com.electroplate.dto.ProdRecordCreateRequest;
import com.electroplate.dto.ProdRecordQuery;
import com.electroplate.dto.ProdRecordVO;
import com.electroplate.entity.ProdRecord;
import com.electroplate.mapper.ProdRecordMapper;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 生产信息记录：查询（K1）、导出（K2）、写入（K3，扫码上料预留）
 */
@Service
@RequiredArgsConstructor
public class ProdRecordService {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /** 导出列头，与列表列完全一致（PRD F1/F4） */
    private static final List<String> HEADERS = Arrays.asList(
            "流水号", "作业员", "吊架号", "投料槽位", "投料时间", "进线时间", "出线时间",
            "批号(左)", "料号(左)", "规格(左)", "阻值(左)", "精度(左)", "数量(左)",
            "批号(右)", "料号(右)", "规格(右)", "阻值(右)", "精度(右)", "数量(右)", "状态");

    /** 导出行数防御上限 */
    private static final int EXPORT_MAX_ROWS = 100_000;

    private final ProdRecordMapper prodRecordMapper;

    /**
     * K1 分页查询：投料时间倒序（并列时 id 倒序），默认全量
     */
    public Page<ProdRecordVO> page(ProdRecordQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 20 : Math.min(query.getSize(), 100);
        Page<ProdRecord> p = prodRecordMapper.selectPage(new Page<>(page, size), buildWrapper(query));
        Page<ProdRecordVO> result = new Page<>(p.getCurrent(), p.getSize(), p.getTotal());
        result.setRecords(p.getRecords().stream().map(ProdRecordVO::of).collect(Collectors.toList()));
        return result;
    }

    /**
     * K2 导出：查询条件与排序同 K1，不分页（R4 所见即所得），返回 xlsx 字节
     */
    public byte[] export(ProdRecordQuery query) {
        LambdaQueryWrapper<ProdRecord> wrapper = buildWrapper(query);
        wrapper.last("LIMIT " + (EXPORT_MAX_ROWS + 1));
        List<ProdRecord> records = prodRecordMapper.selectList(wrapper);
        if (records.size() > EXPORT_MAX_ROWS) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "导出结果超过 " + EXPORT_MAX_ROWS + " 条，请缩小查询范围");
        }
        try (XSSFWorkbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            XSSFSheet sheet = workbook.createSheet("生产记录");
            // 统一列宽：约 5cm（1 字符≈7px，5cm=189px → (189-5)/7*256 ≈ 6732）
            final int colWidth = 6732;
            // 表头样式：14 号加粗 + 浅灰底 + 居中 + 细边框，行高加大
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 14);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(headerStyle);

            // 数据单元格样式：细边框 + 垂直居中
            CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            applyThinBorder(dataStyle);

            Row header = sheet.createRow(0);
            header.setHeightInPoints((short) 24);
            for (int i = 0; i < HEADERS.size(); i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(HEADERS.get(i));
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, colWidth);
            }
            int rowIdx = 1;
            for (ProdRecord r : records) {
                Row row = sheet.createRow(rowIdx++);
                List<String> values = rowValues(ProdRecordVO.of(r));
                for (int i = 0; i < values.size(); i++) {
                    Cell cell = row.createCell(i);
                    cell.setCellValue(values.get(i));
                    cell.setCellStyle(dataStyle);
                }
            }
            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成 Excel 失败：" + e.getMessage());
        }
    }

    /** 给单元格样式加四边细边框 */
    private void applyThinBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * K3 新增记录：流水号服务端生成（PR+yyyyMMdd-XXXX，冲突重试 3 次），投料时间取当前时间
     */
    public ProdRecordVO create(ProdRecordCreateRequest request) {
        ProdRecord record = new ProdRecord();
        record.setOperator(emptyToNull(request.getOperator()));
        record.setRackNo(request.getRackNo().trim());
        record.setFeedSlot(emptyToNull(request.getFeedSlot()));
        record.setFeedTime(LocalDateTime.now());
        record.setInTime(request.getInTime());
        record.setOutTime(request.getOutTime());
        record.setBatchNoL(emptyToNull(request.getBatchNoL()));
        record.setItemNoL(emptyToNull(request.getItemNoL()));
        record.setSpecL(emptyToNull(request.getSpecL()));
        record.setResistanceL(emptyToNull(request.getResistanceL()));
        record.setToleranceL(emptyToNull(request.getToleranceL()));
        record.setQtyL(request.getQtyL());
        record.setBatchNoR(emptyToNull(request.getBatchNoR()));
        record.setItemNoR(emptyToNull(request.getItemNoR()));
        record.setSpecR(emptyToNull(request.getSpecR()));
        record.setResistanceR(emptyToNull(request.getResistanceR()));
        record.setToleranceR(emptyToNull(request.getToleranceR()));
        record.setQtyR(request.getQtyR());

        for (int attempt = 0; attempt <= 3; attempt++) {
            record.setSerialNo(nextSerialNo());
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            try {
                prodRecordMapper.insert(record);
                return ProdRecordVO.of(prodRecordMapper.selectById(record.getId()));
            } catch (DuplicateKeyException e) {
                // 流水号唯一键冲突，重试
            }
        }
        throw new BusinessException(ErrorCode.INTERNAL_ERROR, "流水号生成失败，请稍后重试");
    }

    /**
     * K1/K2 共用查询条件：区间按投料时间（R2），批号左右任一包含匹配（R3），吊架号精确
     */
    private LambdaQueryWrapper<ProdRecord> buildWrapper(ProdRecordQuery query) {
        LambdaQueryWrapper<ProdRecord> wrapper = new LambdaQueryWrapper<>();
        if (query.getFeedTimeStart() != null && query.getFeedTimeEnd() != null
                && query.getFeedTimeEnd().isBefore(query.getFeedTimeStart())) {
            throw new BusinessException(ErrorCode.PARAM_INVALID, "投料时间区间的开始时间不能晚于结束时间");
        }
        wrapper.ge(query.getFeedTimeStart() != null, ProdRecord::getFeedTime, query.getFeedTimeStart());
        wrapper.le(query.getFeedTimeEnd() != null, ProdRecord::getFeedTime, query.getFeedTimeEnd());
        if (StringUtils.hasText(query.getBatchNo())) {
            String kw = query.getBatchNo().trim();
            wrapper.and(w -> w.like(ProdRecord::getBatchNoL, kw).or().like(ProdRecord::getBatchNoR, kw));
        }
        wrapper.eq(StringUtils.hasText(query.getRackNo()), ProdRecord::getRackNo, query.getRackNo());
        wrapper.orderByDesc(ProdRecord::getFeedTime).orderByDesc(ProdRecord::getId);
        return wrapper;
    }

    /** 导出行取值：与表头顺序一一对应，时间格式化、空值为空串 */
    private List<String> rowValues(ProdRecordVO vo) {
        List<String> values = new ArrayList<>();
        values.add(s(vo.getSerialNo()));
        values.add(s(vo.getOperator()));
        values.add(s(vo.getRackNo()));
        values.add(s(vo.getFeedSlot()));
        values.add(t(vo.getFeedTime()));
        values.add(t(vo.getInTime()));
        values.add(t(vo.getOutTime()));
        values.add(s(vo.getBatchNoL()));
        values.add(s(vo.getItemNoL()));
        values.add(s(vo.getSpecL()));
        values.add(s(vo.getResistanceL()));
        values.add(s(vo.getToleranceL()));
        values.add(vo.getQtyL() == null ? "" : String.valueOf(vo.getQtyL()));
        values.add(s(vo.getBatchNoR()));
        values.add(s(vo.getItemNoR()));
        values.add(s(vo.getSpecR()));
        values.add(s(vo.getResistanceR()));
        values.add(s(vo.getToleranceR()));
        values.add(vo.getQtyR() == null ? "" : String.valueOf(vo.getQtyR()));
        values.add("DONE".equals(vo.getStatus()) ? "已完成" : "正在加工");
        return values;
    }

    private String s(String v) {
        return v == null ? "" : v;
    }

    private String t(LocalDateTime v) {
        return v == null ? "" : DATE_TIME.format(v);
    }

    private String emptyToNull(String v) {
        if (v == null) {
            return null;
        }
        String trimmed = v.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    /**
     * 生成下一个流水号：PR + yyyyMMdd + - + 4 位流水（当日最大 +1）
     */
    private String nextSerialNo() {
        String prefix = "PR" + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        QueryWrapper<ProdRecord> wrapper = new QueryWrapper<ProdRecord>();
        wrapper.select("MAX(serial_no) AS serial_no").likeRight("serial_no", prefix);
        ProdRecord max = prodRecordMapper.selectOne(wrapper);
        int next = 1;
        if (max != null && max.getSerialNo() != null) {
            String tail = max.getSerialNo().substring(prefix.length());
            next = Integer.parseInt(tail.replaceAll("\\D", "")) + 1;
        }
        return prefix + "-" + String.format("%04d", next);
    }
}
