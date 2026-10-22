package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationSourceResultBO {
    private Long projectId;
    private String projectTitle;
    private Long itemId;
    private String itemTitle;
    private NotificationTaskSlotBO taskSlot;
}
