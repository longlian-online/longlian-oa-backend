package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "当前用户邮件通知配置")
public class UserEmailNotificationsVO {
    @Schema(description = "是否开启邮件通知，未配置时为 true", requiredMode = Schema.RequiredMode.REQUIRED)
    private boolean enabled;
}
