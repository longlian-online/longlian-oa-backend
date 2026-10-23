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
public class ResourceAbandonParamsBO {
    private Long orgId;
    /** 按业务对象废弃时使用，例如 task_submit。 */
    private String bizType;
    private List<Long> bizIds;
    /** 按当前封面、缩略图等文件 ID 废弃。 */
    private List<Long> resourceIds;
}
