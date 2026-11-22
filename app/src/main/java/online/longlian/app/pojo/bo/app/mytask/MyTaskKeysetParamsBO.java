package online.longlian.app.pojo.bo.app.mytask;

import online.longlian.app.common.enumeration.MyTaskBucket;

import java.time.LocalDateTime;

/**
 * 我的待办键集分页入参：按 (claimed_at, id) 升序；bucket 为空时两个分组都返回。
 */
public record MyTaskKeysetParamsBO(Long userId, Long orgId, MyTaskBucket bucket,
                                  LocalDateTime afterClaimedAt, Long afterInstanceId, int limit) {
}
