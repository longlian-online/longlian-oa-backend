package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 把一份未绑定上传激活到目标业务对象。当前业务类型可以和目标业务类型不同。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceActivateUnboundParamsBO {

    private Long resourceId;
    private Long creatorId;
    private Long orgId;
    private String currentBizType;
    private String targetBizType;
    private Long targetBizId;
}
