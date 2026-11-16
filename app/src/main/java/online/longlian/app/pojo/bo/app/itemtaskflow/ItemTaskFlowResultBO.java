package online.longlian.app.pojo.bo.app.itemtaskflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import online.longlian.common.enumeration.ItemStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTaskFlowResultBO {

    private String itemTitle;

    private ItemStatus status;

    private LocalDateTime completedAt;

    private String name;

    private String description;

    private Boolean hasSubmissionRecord;

    private List<ItemTaskNodeResultBO> nodes;
}
