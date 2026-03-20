package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
public enum SortDirection {

    @Schema(description = "倒序")
    DESC,

    @Schema(description = "正序")
    ASC;
}