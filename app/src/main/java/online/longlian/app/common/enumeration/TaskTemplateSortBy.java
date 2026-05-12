package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

public enum TaskTemplateSortBy {

    @Schema(description = "创建时间")
    CREATED_AT,

    @Schema(description = "引用次数")
    REF_COUNT;
}
