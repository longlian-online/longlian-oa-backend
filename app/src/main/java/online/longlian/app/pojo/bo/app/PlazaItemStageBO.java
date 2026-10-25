package online.longlian.app.pojo.bo.app;

import lombok.Data;

/**
 * 任务广场条目阶段行（自定义 SQL 结果）：条目内一个阶段的 sort 值。
 * <p>
 * 用于在 Java 侧推导 stageIndex（当前阶段在全部阶段中的位次）与 stageTotal（阶段总数），
 * 不依赖 sort 的计数基准（0 或 1 起始）与连续性。
 */
@Data
public class PlazaItemStageBO {

    private Long itemId;

    private Integer sort;
}
