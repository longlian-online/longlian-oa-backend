package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.ProjectStatus;

import java.time.LocalDateTime;

@Data
@Schema(description = "企划管理列表信息")
public class ProjectAdminInfoVO {

    @Schema(description = "企划ID")
    private Long id;

    @Schema(description = "企划标题")
    private String title;

    @Schema(description = "企划类型ID")
    private Long typeId;

    @Schema(description = "企划类型名称")
    private String typeName;

    @Schema(description = "企划状态：IN_PROGRESS-进行中，COMPLETED-已完成，ARCHIVED-已归档")
    private ProjectStatus status;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorNickname;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
