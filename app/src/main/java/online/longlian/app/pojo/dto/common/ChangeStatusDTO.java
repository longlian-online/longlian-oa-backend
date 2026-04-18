package online.longlian.app.pojo.dto.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.generator.enumeration.Status;

@Data
@Schema(description = "改变状态参数")
public class ChangeStatusDTO {

    @Schema(description = "目标状态：1-启用，0-禁用")
    private Status status;
}
