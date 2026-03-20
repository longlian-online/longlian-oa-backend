package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "创建文件上传请求参数")
public class CreateFileReqDTO {

    @NotBlank(message = "原始文件名不能为空")
    @Size(max = 255, message = "文件名长度不能超过255")
    @Schema(description = "原始文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @NotBlank(message = "文件扩展名不能为空")
    @Schema(description = "文件扩展名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileExt;

    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件大小(字节)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    @NotBlank(message = "文件MIME类型不能为空")
    @Schema(description = "文件MIME类型", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileMime;

    @NotBlank(message = "业务类型不能为空")
    @Schema(description = "业务类型(avatar/cover/task_submit)", requiredMode = Schema.RequiredMode.REQUIRED)
    private String bizType;

    @NotNull(message = "业务ID不能为空")
    @Schema(description = "业务ID(用户ID/组织ID/任务ID)", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long bizId;
}