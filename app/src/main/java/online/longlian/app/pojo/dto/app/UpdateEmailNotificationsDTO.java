package online.longlian.app.pojo.dto.app;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "当前用户邮件通知配置")
public class UpdateEmailNotificationsDTO {
    @NotNull(message = "邮件通知开关不能为空")
    @Schema(description = "是否接收前置任务完成和项目完成邮件，不影响站内通知和验证码邮件", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;
}
