package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BaseTaskCreateParamsBO {
    private Long orgId;
    private Long creatorId;
    private String name;
    private String description;
    private Long iconFileId;
    private String metaSchema;
}
