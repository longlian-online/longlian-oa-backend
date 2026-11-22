package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.IdempotencySource;

/**
 * 幂等记录写入入参。record 必须与业务写入在同一事务内调用；
 * 同键并发插入撞唯一索引按幂等冲突拒绝，整个事务回滚，业务不重复生效。
 */
public record IdempotencyRecordParamsBO(IdempotencySource source, Long callerId,
                                        String idempotencyKey, String method, String path,
                                        String bodyHash, Object resultData) {
}
