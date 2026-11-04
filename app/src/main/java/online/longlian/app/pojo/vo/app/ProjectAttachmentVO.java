package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

import java.time.LocalDateTime;

@Data
@Schema(description = "企划附件")
public class ProjectAttachmentVO {

    @JsonLongIdString
    @Schema(type = "string", description = "附件ID")
    private Long id;

    @JsonLongIdString
    @Schema(type = "string", description = "文件ID")
    private Long fileId;

    @Schema(description = "原始文件名")
    private String fileName;

    @Schema(description = "文件大小(字节)")
    private Long fileSize;

    @Schema(description = "文件MIME类型")
    private String fileMime;

    @JsonLongIdString
    @Schema(type = "string", description = "上传人ID")
    private Long uploaderId;

    @Schema(description = "上传人展示名")
    private String uploaderName;

    @Schema(description = "上传时间")
    private LocalDateTime createdAt;
}
