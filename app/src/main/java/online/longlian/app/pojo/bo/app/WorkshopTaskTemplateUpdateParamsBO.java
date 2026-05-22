package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTaskTemplateUpdateParamsBO {
    private Long templateId;
    private Long orgId;
    private Long userId;
    private String name;
    private String description;
    private List<WorkshopTaskTemplateNodeCreateParamsBO> nodes;
}
