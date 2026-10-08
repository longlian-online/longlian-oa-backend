package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBindParamsBO {
    private Long resourceId;
    private List<Long> resourceIds;
    private Long replacedResourceId;
    /** Must match the business type stored when the file was created. */
    private String bizType;
    private Long bizId;
    private Long creatorId;
    private Long orgId;
    /** Already activated resources with this bizId stay bound. Single-slot replacement leaves this false. */
    private Boolean reuseBound;
}
