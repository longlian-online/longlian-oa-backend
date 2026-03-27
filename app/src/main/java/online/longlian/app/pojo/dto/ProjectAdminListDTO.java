package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;
import online.longlian.app.common.enumeration.SortDirection;

import java.time.LocalDateTime;

@Data
@Schema(description = "企划管理列表查询请求参数")
public class ProjectAdminListDTO {

    @Min(value = 1, message = "页码不能小于 1")
    @Schema(description = "当前页码")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数最小为 1")
    @Max(value = 100, message = "每页条数最大为 100")
    @Schema(description = "每页条数")
    private Integer pageSize = 10;

    @Size(max = 100, message = "搜索关键词长度不能超过 100 个字符")
    @Schema(description = "企划标题模糊搜索关键词")
    private String keyword;

    @Schema(description = "企划类型ID（精确筛选）")
    private Long typeId;

    @Schema(description = "创建时间-起始")
    private LocalDateTime startCreatedTime;

    @Schema(description = "创建时间-结束")
    private LocalDateTime endCreatedTime;

    @Schema(description = "排序方式，默认创建时间倒序")
    private SortDirection orderDir = SortDirection.DESC;
}
