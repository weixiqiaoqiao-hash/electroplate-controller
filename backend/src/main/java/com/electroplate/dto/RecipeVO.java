package com.electroplate.dto;

import com.electroplate.entity.Recipe;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 配方响应视图（I1/I2/I3/I4/I5/I6 统一返回结构）
 */
@Data
public class RecipeVO {

    /** 主键 */
    private Long id;

    /** 配方编号 RECyyyyMMdd-XXXX */
    private String recipeCode;

    /** 类别 */
    private String category;

    /** 规格 */
    private String spec;

    /** 阻值下限(Ω) */
    private BigDecimal resMin;

    /** 阻值上限(Ω) */
    private BigDecimal resMax;

    /** 数量下限(pcs) */
    private Integer qtyMin;

    /** 数量上限(pcs) */
    private Integer qtyMax;

    /** 镍段参数 */
    private SectionVO nickel;

    /** 锡段参数 */
    private SectionVO tin;

    /** 状态：ENABLED 启用 / DISABLED 停用 */
    private String status;

    /** 乐观锁版本号，修改时需回传 */
    private Integer version;

    /** 创建人，账号体系上线前留空 */
    private String createdBy;

    /** 创建时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createdAt;

    /** 修改人，账号体系上线前留空 */
    private String updatedBy;

    /** 修改时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime updatedAt;

    /**
     * 实体转视图对象
     */
    public static RecipeVO of(Recipe r) {
        RecipeVO vo = new RecipeVO();
        vo.setId(r.getId());
        vo.setRecipeCode(r.getRecipeCode());
        vo.setCategory(r.getCategory());
        vo.setSpec(r.getSpec());
        vo.setResMin(r.getResMin());
        vo.setResMax(r.getResMax());
        vo.setQtyMin(r.getQtyMin());
        vo.setQtyMax(r.getQtyMax());

        SectionVO nickel = new SectionVO();
        nickel.setSeg1Current(r.getNiSeg1Current());
        nickel.setSeg1Time(r.getNiSeg1Time());
        nickel.setSeg2Current(r.getNiSeg2Current());
        nickel.setSeg2Time(r.getNiSeg2Time());
        nickel.setSeg3Current(r.getNiSeg3Current());
        nickel.setSeg3Time(r.getNiSeg3Time());
        nickel.setTotalTime(r.getNiTotalTime());
        nickel.setProtectCurrent(r.getNiProtectCurrent());
        nickel.setAlarmVoltage(r.getNiAlarmVoltage());
        vo.setNickel(nickel);

        SectionVO tin = new SectionVO();
        tin.setSeg1Current(r.getSnSeg1Current());
        tin.setSeg1Time(r.getSnSeg1Time());
        tin.setSeg2Current(r.getSnSeg2Current());
        tin.setSeg2Time(r.getSnSeg2Time());
        tin.setSeg3Current(r.getSnSeg3Current());
        tin.setSeg3Time(r.getSnSeg3Time());
        tin.setTotalTime(r.getSnTotalTime());
        tin.setProtectCurrent(r.getSnProtectCurrent());
        tin.setAlarmVoltage(r.getSnAlarmVoltage());
        vo.setTin(tin);

        vo.setStatus(r.getStatus() != null && r.getStatus() == 1 ? "ENABLED" : "DISABLED");
        vo.setVersion(r.getVersion());
        vo.setCreatedBy(r.getCreatedBy());
        vo.setCreatedAt(r.getCreatedAt());
        vo.setUpdatedBy(r.getUpdatedBy());
        vo.setUpdatedAt(r.getUpdatedAt());
        return vo;
    }

    /**
     * 工艺段参数视图（镍段/锡段各一组，字段与镍/锡段实体列一一对应）
     */
    @Data
    public static class SectionVO {

        /** 一段电流(A) */
        private BigDecimal seg1Current;

        /** 一段时间(Sec) */
        private Integer seg1Time;

        /** 二段电流(A) */
        private BigDecimal seg2Current;

        /** 二段时间(Sec) */
        private Integer seg2Time;

        /** 三段电流(A) */
        private BigDecimal seg3Current;

        /** 三段时间(Sec) */
        private Integer seg3Time;

        /** 总时间(Sec)，恒等于三段时间之和，服务端计算 */
        private Integer totalTime;

        /** 保护电流(A) */
        private BigDecimal protectCurrent;

        /** 报警电压(V) */
        private BigDecimal alarmVoltage;
    }
}
