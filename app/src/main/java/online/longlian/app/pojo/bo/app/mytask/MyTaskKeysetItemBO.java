package online.longlian.app.pojo.bo.app.mytask;

import online.longlian.app.common.enumeration.MyTaskBucket;
import online.longlian.common.enumeration.TaskInstanceStatus;

import java.time.LocalDateTime;

/**
 * 我的待办键集分页条目：任务均为已接取（CLAIMED），revision 供接取/提交等操作的乐观锁使用。
 */
public record MyTaskKeysetItemBO(Long instanceId, String nodeName,
                                 Long itemId, String itemTitle, Long projectId, String projectName,
                                 TaskInstanceStatus taskStatus, Long revision, LocalDateTime claimedAt,
                                 MyTaskBucket bucket) {
}
