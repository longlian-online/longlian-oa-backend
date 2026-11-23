package online.longlian.app.pojo.bo.open;

import online.longlian.app.common.enumeration.PlazaStageScope;

/**
 * 开放面任务广场键集列表入参。组织上下文来自密钥，不接受调用方指定。
 */
public record OpenTaskPlazaListParamsBO(Long orgId, Long baseTaskId, String keyword,
                                        PlazaStageScope stageScope, OpenCursorBO cursor, int limit) {
}
