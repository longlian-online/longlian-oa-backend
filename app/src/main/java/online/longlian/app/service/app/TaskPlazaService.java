package online.longlian.app.service.app;

import online.longlian.app.pojo.bo.app.TaskPlazaParamsBO;
import online.longlian.app.pojo.vo.app.PlazaProjectGroupVO;
import online.longlian.app.pojo.vo.common.PageResultVO;

/**
 * 任务广场（缺人任务）服务接口。
 * <p>
 * 汇总当前组织内所有「缺人」的任务实例，按企划分页分组返回：
 * <ul>
 *   <li>缺人 = 实例处于 PENDING（待接取）且无接取人；</li>
 *   <li>ALL 包含所有阶段，CURRENT 仅包含条目首个未完成阶段；</li>
 *   <li>仅统计进行中（IN_PROGRESS）条目与进行中、启用的企划。</li>
 * </ul>
 */
public interface TaskPlazaService {

    /**
     * 分页查询缺人任务，按企划分组。
     *
     * @param params 查询参数（组织、关键词、类型、排序、分页）
     * @return 按企划分组的缺人任务分页结果，total 为缺人企划数
     */
    PageResultVO<PlazaProjectGroupVO> getTaskPlaza(TaskPlazaParamsBO params);
}
