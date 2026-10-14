package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.app.common.enumeration.SortDirection;

@Data
@Schema(description = "我的待办查询参数")
public class MyTaskListDTO {

    @NotNull(message = "分组不能为空")
    @Schema(description = "分组：SUBMITTABLE-可提交，WAITING-等前置", requiredMode = Schema.RequiredMode.REQUIRED)
    private MyTaskBucket bucket;

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码，默认 1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @Schema(description = "每页条数，默认 5")
    private Integer pageSize = 5;

    @Schema(description = "按接取时间排序：ASC-最早接取（默认），DESC-最晚接取")
    private SortDirection sortDirection = SortDirection.ASC;
}
