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
    /** 打回通知的来源是提交记录，额外携带被关联的任务实例ID用于跳转。 */
    private Long taskInstanceId;
    /** 打回意见，随被打回的那一版提交固定下来，不随后续提交变化。 */
    private String reviewComment;

    public NotificationSourceResultBO(Long projectId, String projectTitle, Long itemId, String itemTitle,
                                      NotificationTaskSlotBO taskSlot) {
        this.projectId = projectId;
        this.projectTitle = projectTitle;
        this.itemId = itemId;
        this.itemTitle = itemTitle;
        this.taskSlot = taskSlot;
    }
}
