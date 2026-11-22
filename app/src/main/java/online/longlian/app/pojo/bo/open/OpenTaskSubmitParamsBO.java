package online.longlian.app.pojo.bo.open;

import java.util.Map;

/**
 * 开放面提交任务入参。values 按节点 metaSchema 由站内校验。
 */
public record OpenTaskSubmitParamsBO(Long instanceId, Long userId, Long orgId,
                                     Map<String, Object> values) {
}
