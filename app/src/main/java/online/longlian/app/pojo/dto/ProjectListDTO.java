package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;

@Data
@Schema(description = "企划查询请求参数")
public class ProjectListDTO {

    @NotNull(message = "组织ID不能为空")
    @Schema(description = "组织ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long orgId;

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "搜索关键词（标题/别名）")
    private String keyword;

    @Size(max = 20, message = "企划类型名称长度不能超过20")
    @Schema(description = "企划类型")
    private String projectType;

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Schema(description = "排序字段")
    private SortByTime sortByTime;

    @Schema(description = "排序方式")
    private SortDirection orderDir;
}