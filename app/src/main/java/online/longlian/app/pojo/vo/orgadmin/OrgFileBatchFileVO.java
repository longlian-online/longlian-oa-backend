package online.longlian.app.pojo.vo.orgadmin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import online.longlian.app.common.annotation.JsonLongIdString;

@Data
@Schema(description = "批量下载中的一个提交文件")
public class OrgFileBatchFileVO {

    @JsonLongIdString
    @Schema(type = "string", description = "任务实例 ID")
    private Long taskInstanceId;

    @Schema(description = "提交字段 key")
    private String fieldKey;

    @JsonLongIdString
    @Schema(type = "string", description = "文件 ID")
    private Long fileId;

    @Schema(description = "原文件名")
    private String fileName;

    @Schema(description = "文件大小，字节")
    private Long fileSize;

}
