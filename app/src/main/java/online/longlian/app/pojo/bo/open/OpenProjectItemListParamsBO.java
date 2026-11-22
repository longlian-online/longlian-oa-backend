package online.longlian.app.pojo.bo.open;

import java.time.LocalDateTime;

/**
 * 开放面企划下项目键集列表入参。组织上下文来自密钥，不接受调用方指定。
 */
public record OpenProjectItemListParamsBO(Long projectId, Long orgId, OpenCursorBO cursor,
                                          LocalDateTime updatedSince, int limit) {
}
