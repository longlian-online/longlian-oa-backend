package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@AllArgsConstructor
@Schema(description = "任务大厅任务类型选项")
public class PlazaTaskTypeOptionVO {

    @JsonLongIdString
    @Schema(type = "string", description = "原子任务ID")
    private Long id;

    private String name;
}
