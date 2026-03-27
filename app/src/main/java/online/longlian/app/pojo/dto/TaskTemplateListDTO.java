package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.common.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Schema(description = "任务模板列表查询请求参数")
public class TaskTemplateListDTO {

    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Size(max = 100, message = "搜索关键词长度不能超过 100 个字符")
    @Schema(description = "模板名称模糊搜索关键词")
    private String keyword;

    @Schema(description = "状态精确筛选：ENABLED-启用，DISABLED-禁用，默认查启用")
    private Status status = Status.ENABLED;

    @Schema(description = "创建时间-起始")
    private LocalDateTime startCreatedTime;

    @Schema(description = "创建时间-结束")
    private LocalDateTime endCreatedTime;

    @Schema(description = "排序字段：createdAt-创建时间，refCount-引用次数，默认 refCount")
    private String sortBy = "refCount";

    @Schema(description = "排序方式，默认倒序")
    private SortDirection orderDir = SortDirection.DESC;
}
