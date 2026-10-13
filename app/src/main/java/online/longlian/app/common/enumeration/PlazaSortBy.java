package online.longlian.app.common.enumeration;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务广场（缺人任务）分组排序方式。
 * <p>
 * 分组粒度为企划，组内任务按 阶段序 → 并行组号 → 创建时间 固定排序。
 */
public enum PlazaSortBy {

    @Schema(description = "等待最久优先（按企划内最老缺人任务升序）")
    WAIT,

    @Schema(description = "最新发布优先（按企划内最新缺人任务降序）")
    LATEST,

    @Schema(description = "缺员最多优先（按企划缺人任务数降序）")
    MOST;
}
