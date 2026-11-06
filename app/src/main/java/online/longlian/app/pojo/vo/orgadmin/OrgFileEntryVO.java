package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;
import online.longlian.common.enumeration.OrgFileEntryKind;

import java.time.LocalDateTime;

@Data
@Schema(description = "组织文件页的一条文件夹或提交文件")
public class OrgFileEntryVO {
    @Schema(description = "条目 ID。文件夹为企划或项目 ID，文件为任务实例 ID 与字段 key")
    private String id;

    @Schema(description = "FOLDER-文件夹，FILE-提交文件")
    private OrgFileEntryKind kind;

    @Schema(description = "文件夹名称或文件原名；文件不可用时改为字段名")
    private String name;

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例 ID，仅文件")
    private Long taskInstanceId;

    @Schema(description = "任务名称，仅文件")
    private String taskName;

    @Schema(description = "提交字段名，仅文件")
    private String fieldLabel;

    @JsonLongIdString
    @Schema(type = "string", description = "文件 ID，仅文件")
    private Long fileId;

    @Schema(description = "文件大小，字节；不可用时为空")
    private Long fileSize;

    @Schema(description = "MIME 类型；不可用时为空")
    private String fileMime;

    @Schema(description = "available-可预览和下载，unavailable-资源不可用")
    private String availability;

    @Schema(description = "提交人昵称，仅文件")
    private String submitterName;

    @Schema(description = "提交时间，仅文件")
    private LocalDateTime submittedAt;
}
