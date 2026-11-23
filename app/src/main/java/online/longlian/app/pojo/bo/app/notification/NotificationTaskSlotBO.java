package online.longlian.app.pojo.bo.app.notification;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NotificationTaskSlotBO {
    private String name;
    private int slotIndex;
    private int slotTotal;
    private Integer serialStep;
}
