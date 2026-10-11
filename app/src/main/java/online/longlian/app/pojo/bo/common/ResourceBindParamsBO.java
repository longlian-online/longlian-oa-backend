package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResourceBindParamsBO {
    private Long resourceId;
    private Long replacedResourceId;
    /** 必须与创建文件时写入的业务类型一致。 */
    private String bizType;
    private Long bizId;
    private Long creatorId;
    private Long orgId;
}

