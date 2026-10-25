package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

public enum PlazaStageScope {
    @Schema(description = "所有阶段的待接取任务")
    ALL,
    @Schema(description = "首个未完成阶段的待接取任务")
    CURRENT
}
