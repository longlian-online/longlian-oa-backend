package online.longlian.app.pojo.dto.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.OrgFileBatchTargetKind;

@Data
@Schema(description = "组织文件页批量下载的一项选择")
public class OrgFileBatchTargetDTO {

    @NotNull(message = "下载目标不能为空")
    @Schema(description = "FILE-提交文件，ITEM-项目，PROJECT-企划，TYPE-企划类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private OrgFileBatchTargetKind kind;

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例 ID，仅 FILE")
    private Long taskInstanceId;

    @JsonLongIdString
    @Schema(type = "string", description = "文件 ID，仅 FILE")
    private Long fileId;

    @Schema(description = "提交字段 key，仅 FILE")
    private String fieldKey;

    @JsonLongIdString
    @Schema(type = "string", description = "项目 ID，仅 ITEM")
    private Long itemId;

    @JsonLongIdString
    @Schema(type = "string", description = "企划 ID，仅 PROJECT")
    private Long projectId;

    @JsonLongIdString
    @Schema(type = "string", description = "企划类型 ID，仅 TYPE")
    private Long typeId;
}
