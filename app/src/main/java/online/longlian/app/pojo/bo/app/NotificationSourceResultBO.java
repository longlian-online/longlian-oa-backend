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
    private String applicantName;
    private String requesterName;
    private String targetType;
    private String targetTitle;

    public NotificationSourceResultBO(Long projectId, String projectTitle, Long itemId, String itemTitle,
                                      NotificationTaskSlotBO taskSlot) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.taskSlot = taskSlot;
    }
}
