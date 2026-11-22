package online.longlian.app.pojo.bo.app.item;

import java.time.LocalDateTime;

/**
 * 企划下项目键集分页入参：按 (updated_at, id) 升序，游标与增量起点可单独或组合使用。
 */
public record ItemKeysetListParamsBO(Long projectId, Long orgId,
                                     LocalDateTime afterUpdatedAt, Long afterId,
                                     LocalDateTime updatedSince, int limit) {
}
