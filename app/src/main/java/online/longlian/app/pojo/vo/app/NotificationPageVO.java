package online.longlian.app.pojo.vo.app;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
@Schema(description = "通知分页结果")
public class NotificationPageVO {

    @Schema(description = "通知列表")
    private List<NotificationVO> list;

    @Schema(description = "是否还有下一页")
    private boolean hasNext;

    @Schema(description = "未读通知数")
    private long unreadCount;
}
