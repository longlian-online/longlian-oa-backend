package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.common.enumeration.PlazaSortBy;
import online.longlian.app.pojo.dto.common.PageRequestDTO;

@Data
@Schema(description = "任务广场（缺人任务）查询参数")
@EqualsAndHashCode(callSuper = true)
public class TaskPlazaListDTO extends PageRequestDTO {

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "搜索关键词（匹配企划名、条目名、任务名）")
    private String keyword;

    @Size(max = 20, message = "企划类型名称长度不能超过20")
    @Schema(description = "企划类型名称")
    private String projectType;

    @Schema(description = "排序方式")
    private PlazaSortBy sortBy = PlazaSortBy.WAIT;
}
