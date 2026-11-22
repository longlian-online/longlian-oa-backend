package online.longlian.app.pojo.bo.open;

import online.longlian.app.common.enumeration.MyTaskBucket;

/**
 * 开放面我的待办键集列表入参。组织上下文来自密钥，不接受调用方指定。
 */
public record OpenMyTaskListParamsBO(Long userId, Long orgId, MyTaskBucket bucket,
                                     OpenCursorBO cursor, int limit) {
}
