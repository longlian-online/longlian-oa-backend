package online.longlian.app.pojo.dto.app;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@Data
@Schema(description = "企划查询请求参数")
@EqualsAndHashCode(callSuper = true)
public class ProjectListDTO extends PageRequestDTO {

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "搜索关键词（标题、别名、标签和自定义字段），按加权相关性优先排序")
    private String keyword;

    @Size(max = 20, message = "企划类型名称长度不能超过20")
    @Schema(description = "企划类型")
    private String projectType;

    @Schema(description = "排序字段")
    private SortByTime sortByTime;

    @Schema(description = "排序方式")
    private SortDirection orderDir = SortDirection.DESC;

    @Min(value = 0, message = "offset不能小于0")
    @Schema(description = "显式起始偏移量，传入后以它计算分页窗口（可跨页窗口），忽略 pageNum 的偏移意义")
    private Integer offset;

    @Override
    protected boolean canEqual(final Object other) {
        return other instanceof ProjectListDTO;
    }
}
