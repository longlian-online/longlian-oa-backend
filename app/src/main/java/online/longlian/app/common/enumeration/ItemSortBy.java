package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

public enum ItemSortBy {
    @Schema(description = "自定义顺序")
    MANUAL,
    @Schema(description = "最早发布")
    CREATED_ASC,
    @Schema(description = "最新发布")
    CREATED_DESC,
    @Schema(description = "最早更新")
    UPDATED_ASC,
    @Schema(description = "最近更新")
    UPDATED_DESC
}
