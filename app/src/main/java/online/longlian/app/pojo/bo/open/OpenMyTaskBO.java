package online.longlian.app.pojo.bo.open;

import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;

/**
 * 开放面我的待办条目：密钥用户已接取（CLAIMED）的任务，按接取时间升序。
 */
public record OpenMyTaskBO(Long taskInstanceId, String nodeName,
                           Long itemId, String itemTitle, Long projectId, String projectName,
                           TaskInstanceStatus taskStatus, Long revision, LocalDateTime claimedAt,
                           MyTaskBucket bucket) {
}
