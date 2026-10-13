package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;
import online.longlian.app.pojo.dto.common.PageRequestDTO;
import online.longlian.app.common.enumeration.WorkshopSortBy;

@EqualsAndHashCode(callSuper = true)
@Data
@Schema(description = "工坊中企划查询请求参数")
public class WorkshopListDTO extends PageRequestDTO {

    @Size(max = 50, message = "搜索关键词长度不能超过50")
    @Schema(description = "搜索关键词（标题）")
    private String keyword;

    @Size(max = 20, message = "企划类型名称长度不能超过20，为空则查询所有类型的企划")
    @Schema(description = "企划类型")
    private String projectType;

    @Schema(description = "选择是否为我创建的企划，false=查询全部")
    private Boolean isMyCreated;

    @NotNull
    @Schema(description = "排序：RECENT_SUBMISSION-最近提交，CREATED_DESC-最新创建，CREATED_ASC-最早创建，默认最近提交")
    private WorkshopSortBy sortBy = WorkshopSortBy.RECENT_SUBMISSION;
}
