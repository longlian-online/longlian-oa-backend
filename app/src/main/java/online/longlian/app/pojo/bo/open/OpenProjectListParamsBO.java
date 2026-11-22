package online.longlian.app.pojo.bo.open;

import java.time.LocalDateTime;

/**
 * 企划增量/游标列表入参。组织上下文来自密钥，不接受调用方指定。
 */
public record OpenProjectListParamsBO(Long userId, Long orgId, int limit,
                                      OpenCursorBO cursor, LocalDateTime updatedSince) {
}
