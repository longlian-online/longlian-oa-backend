package online.longlian.app.pojo.bo.open;

import online.longlian.common.enumeration.ItemStatus;

import java.time.LocalDateTime;

/**
 * 开放面企划下项目条目：不含节点结构（节点走任务流接口）。
 */
public record OpenProjectItemBO(Long id, String title, ItemStatus status, String currentNodeName,
                                Integer progressPercent, Boolean hasSubmissionRecord,
                                LocalDateTime createdAt, LocalDateTime updatedAt) {
}
