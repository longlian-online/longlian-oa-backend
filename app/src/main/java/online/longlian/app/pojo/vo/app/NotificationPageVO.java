package online.longlian.app.pojo.vo.app;

import lombok.AllArgsConstructor;
import lombok.Data;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Data
@AllArgsConstructor
public class NotificationPageVO {
    private List<NotificationVO> list;
    private boolean hasNext;
    private long unreadCount;
}
