package online.longlian.app.pojo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "打回任务请求参数")
public class TaskRejectDTO {

    @NotBlank(message = "打回意见不能为空")
    @Size(max = 500, message = "打回意见不能超过 500 个字符")
    @Schema(description = "打回意见", requiredMode = Schema.RequiredMode.REQUIRED)
    private String reviewComment;
}
