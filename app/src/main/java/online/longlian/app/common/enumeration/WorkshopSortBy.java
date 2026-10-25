package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

public enum WorkshopSortBy {
    @Schema(description = "最近提交，未提交的企划排在最后")
    RECENT_SUBMISSION,
    @Schema(description = "最新创建")
    CREATED_DESC,
    @Schema(description = "最早创建")
    CREATED_ASC
}
