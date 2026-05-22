package online.longlian.app.pojo.bo.app;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.app.pojo.bo.PageParamsBO;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopTaskTemplateListParamsBO {
    private Long orgId;
    private Long userId;
    private String keyword;
    private Boolean isMyCreated;
    private PageParamsBO page;
}
