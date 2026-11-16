package online.longlian.app.pojo.bo.app.projectworkshop;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.TaskTemplateScope;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTaskTemplateResultBO {

    private Long id;

    private String name;

    private String description;

    private TaskTemplateScope scope;

    private Integer taskCount;

    private List<WorkshopTaskTemplateNodeResultBO> nodes;

    private Boolean isMine;
}
