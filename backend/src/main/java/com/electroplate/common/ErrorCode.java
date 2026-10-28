package com.electroplate.common;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 业务错误码枚举：业务码与 HTTP 状态码同步语义化
 */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    /** 成功 */
    SUCCESS(0, 200, "success"),

    /** 参数校验失败，message 指明具体字段 */
    PARAM_INVALID(40000, 400, "参数校验失败"),

    /** 配方不存在 */
    RECIPE_NOT_FOUND(40401, 404, "配方不存在"),

    /** 槽位不存在 */
    SLOT_NOT_FOUND(40402, 404, "槽位不存在"),

    /** 范围重叠冲突，data 返回冲突配方编号 */
    RANGE_OVERLAP(40901, 409, "范围重叠冲突"),

    /** 数据已被他人修改（乐观锁失败） */
    CONCURRENT_MODIFY(40902, 409, "数据已被他人修改，请刷新后重试"),

    /** 字典项已被配方引用，不可删除 */
    DICT_IN_USE(40903, 409, "字典项已被配方引用，不可删除"),

    /** 槽位名称已存在（含停用槽位） */
    SLOT_NAME_DUP(40904, 409, "槽位名称已存在"),

    /** 工序序号已被其他槽位占用（含停用槽位） */
    SLOT_SORT_NO_DUP(40905, 409, "工序序号已被其他槽位占用"),

    /** 服务器内部错误 */
    INTERNAL_ERROR(50000, 500, "服务器内部错误");

    /** 业务码 */
    private final int code;

    /** 对应的 HTTP 状态码 */
    private final int httpStatus;

    /** 默认提示信息 */
    private final String message;
}
