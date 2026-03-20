package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

public enum SortByTime {

    @Schema(description = "按创建时间排序")
    CREATE,

    @Schema(description = "按更新时间排序")
    UPDATE;
}
