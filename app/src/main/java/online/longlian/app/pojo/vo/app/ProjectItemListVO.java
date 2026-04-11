package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.ItemStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "项目列表信息")
public class ProjectItemListVO {

    @Schema(description = "项目ID")
    private Long id;

    @Schema(description = "项目标题")
    private String title;

    @Schema(description = "项目状态")
    private ItemStatus status;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "更新时间")
    private LocalDateTime updatedAt;

    @Schema(description = "进度百分比（0-100）")
    private Integer progressPercent;

    @Schema(description = "当前节点名称")
    private String currentNodeName;

    @Schema(description = "节点列表")
    private List<ProjectItemNodeVO> nodes;
}
