package online.longlian.app.pojo.bo.app.itemtaskflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItemTaskFlowResultBO {

    private String itemTitle;

    private String name;

    private String description;

    private Boolean hasSubmissionRecord;

    private List<ItemTaskNodeResultBO> nodes;
}
