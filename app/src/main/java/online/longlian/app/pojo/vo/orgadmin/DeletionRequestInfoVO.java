package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.DeletionTargetType;

import java.time.LocalDateTime;

@Data
@Schema(description = "删除申请")
public class DeletionRequestInfoVO {

    @JsonLongIdString
    @Schema(type = "string", description = "申请ID")
    private Long id;

    @Schema(description = "目标类型：PROJECT-企划，ITEM-项目")
    private DeletionTargetType targetType;

    @JsonLongIdString
    @Schema(type = "string", description = "目标ID")
    private Long targetId;

    @JsonLongIdString
    @Schema(type = "string", description = "所属企划ID")
    private Long projectId;

    @Schema(description = "名称")
    private String title;

    @Schema(description = "删除原因")
    private String reason;

    @Schema(description = "申请人")
    private String requesterName;

    @Schema(description = "申请时间")
    private LocalDateTime createdAt;
}
