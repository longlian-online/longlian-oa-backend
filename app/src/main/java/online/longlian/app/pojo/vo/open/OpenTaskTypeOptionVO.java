package online.longlian.app.pojo.vo.open;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 开放面任务大厅任务类型选项。字段冻结：只增不删。
 */
@Data
@AllArgsConstructor
@Schema(description = "开放接口任务类型选项")
public class OpenTaskTypeOptionVO {

    @Schema(description = "原子任务ID")
    private Long id;

    @Schema(description = "原子任务名")
    private String name;
}
