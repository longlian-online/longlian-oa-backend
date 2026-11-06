package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.FileProcessStatus;

import java.time.LocalDateTime;

/** 事务内锁定的资源归属，供业务模块决定如何改绑。 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LockedResourceBO {

    private Long id;
    private Long orgId;
    private Long creatorId;
    private String bizType;
    private Long bizId;
    private FileProcessStatus processStatus;
    private LocalDateTime storageCleanedAt;
}
