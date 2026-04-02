package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.enumeration.ItemStatus;
import online.longlian.app.common.enumeration.SortByTime;
import online.longlian.app.common.enumeration.SortDirection;

@Data
@Schema(description = "项目列表查询参数")
public class ProjectItemListDTO {

    @Min(value = 1, message = "页码不能小于1")
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为1")
    @Max(value = 100, message = "每页条数最大为100")
    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "项目标题关键词（模糊匹配）")
    private String keyword;

    @Schema(description = "项目状态筛选：IN_PROGRESS-进行中，COMPLETED-已完成，PUBLISHED-已公布；默认进行中")
    private ItemStatus status = ItemStatus.IN_PROGRESS;

    @Schema(description = "排序字段：CREATE-创建时间，UPDATE-更新时间")
    private SortByTime sortByTime = SortByTime.UPDATE;

    @Schema(description = "排序方式：DESC-倒序，ASC-正序")
    private SortDirection orderDir = SortDirection.DESC;
}
