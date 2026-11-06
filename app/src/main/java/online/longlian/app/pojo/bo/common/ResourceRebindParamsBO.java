package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 改绑一份已激活资源。probe 为 true 时先核对存储对象，且不刷新 updatedAt。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceRebindParamsBO {

    private Long resourceId;
    private String currentBizType;
    private Long currentBizId;
    private String targetBizType;
    private Long targetBizId;
    private boolean probe;
}
