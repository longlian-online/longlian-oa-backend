package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
@Schema(description = "绑定企划附件请求参数")
public class ProjectAttachmentCreateDTO {

    @NotNull(message = "文件ID不能为空")
    @Positive(message = "文件ID必须大于0")
    @Schema(description = "已上传的文件ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileId;
}
