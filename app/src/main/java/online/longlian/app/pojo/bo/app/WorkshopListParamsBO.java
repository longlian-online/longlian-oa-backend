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
public class WorkshopListParamsBO {
    private Long userId;
    private Long orgId;
    private String keyword;
    private String projectType;
    private Boolean isMyCreated;
    private PageParamsBO page;
}
