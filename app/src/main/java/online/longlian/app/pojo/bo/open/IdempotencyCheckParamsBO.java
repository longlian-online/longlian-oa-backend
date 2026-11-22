package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.IdempotencySource;

/**
 * 幂等查重入参。命中判定除幂等键外还比对方法、路径与请求体哈希：
 * 同键挪用到别的接口就是冲突，不静默放过。
 */
public record IdempotencyCheckParamsBO(IdempotencySource source, Long callerId,
                                       String idempotencyKey, String method, String path,
                                       String bodyHash) {
}
