package online.longlian.app.pojo.vo.orgadmin;

import online.longlian.app.common.annotation.JsonLongIdString;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.common.enumeration.Status;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "企划类型管理列表信息")
public class ProjectTypeAdminVO {

    @JsonLongIdString
    @Schema(type = "string", description = "企划类型ID")
    private Long id;

    @Schema(description = "类型名称")
    private String name;

    @Schema(description = "优先级，仅影响用户端类型列表顺序，越大越靠前")
    private Integer priority;

    @Schema(description = "引用次数（该类型被企划引用的次数）")
    private Integer refCount;

    @Schema(description = "状态：ENABLED-启用，DISABLED-禁用")
    private Status status;

    @JsonLongIdString
    @Schema(type = "string", description = "创建人ID")
    private Long creatorId;

    @Schema(description = "创建人昵称")
    private String creatorNickname;

    @Schema(description = "创建时间")
    private LocalDateTime createdAt;

    @Schema(description = "默认自定义表单")
    private List<ProjectTypeCustomFieldVO> customFields;
}
