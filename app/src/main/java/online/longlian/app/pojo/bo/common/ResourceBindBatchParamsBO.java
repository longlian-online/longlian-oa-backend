package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** 将多份上传绑定到同一业务对象，不替换、不废弃其他资源。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBindBatchParamsBO {
    private List<Long> resourceIds;
    /** 必须与每份文件创建时写入的业务类型一致。 */
    private String bizType;
    private Long bizId;
    private Long creatorId;
    private Long orgId;
}
