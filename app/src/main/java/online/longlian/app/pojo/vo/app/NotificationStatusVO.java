package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@Schema(description = "通知未读状态")
public class NotificationStatusVO {

    @Schema(description = "是否存在未读通知")
    private boolean hasUnread;
}
