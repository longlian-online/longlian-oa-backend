package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "创建文件上传请求参数")
public class CreateFileReqDTO {

    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "文件扩展名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileExt;

    @Schema(description = "文件大小(字节)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    @Schema(description = "文件MIME类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileMime;

    @Schema(description = "业务类型(avatar/cover/task_submit)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;

    @Schema(description = "业务ID(用户ID/组织ID/任务ID)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bizId;
}