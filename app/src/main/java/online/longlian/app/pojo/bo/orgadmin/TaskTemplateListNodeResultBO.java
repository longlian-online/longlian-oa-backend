package online.longlian.app.pojo.bo.orgadmin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 列表卡片用的阶段预览。不含提交字段，编辑模板仍走详情。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskTemplateListNodeResultBO {
    private Long baseTaskId;
    private String baseTaskName;
    private String baseTaskIcon;
    private Integer sort;
    private Integer parallelSort;
}
