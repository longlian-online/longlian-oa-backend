package online.longlian.app.pojo.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.enumeration.Status;

import java.time.LocalDateTime;

@Data
@Schema(description = "企划类型管理列表信息")
public class ProjectTypeAdminVO {

    @Schema(description = "企划类型ID")
    private Long id;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "状态：ENABLED-启用，DISABLED-禁用")
    private Status status;

    @Schema(description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorNickname;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;
}
