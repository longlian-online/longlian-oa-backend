package online.longlian.app.pojo.bo.app.taskplaza;

import online.longlian.app.common.enumeration.PlazaStageScope;

import java.time.LocalDateTime;

/**
 * 任务广场键集分页入参：按企划 (updated_at, id) 升序；筛选语义与 {@link TaskPlazaParamsBO} 一致。
 */
public record TaskPlazaKeysetParamsBO(Long orgId, Long baseTaskId, String keyword,
                                      PlazaStageScope stageScope,
                                      LocalDateTime afterProjectUpdatedAt, Long afterProjectId, int limit) {
}
