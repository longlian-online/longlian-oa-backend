package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 「我的待办」分组。
 */
public enum MyTaskBucket {

    @Schema(description = "可提交：同一项目中前置阶段（sort 更小）全部已完成")
    SUBMITTABLE,

    @Schema(description = "等前置：存在未完成的前置阶段")
    WAITING;
}
