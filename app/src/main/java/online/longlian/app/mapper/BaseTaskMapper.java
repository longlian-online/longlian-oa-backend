package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.BaseTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import online.longlian.app.pojo.bo.orgadmin.basetask.PlazaTaskTypeOptionResultBO;

/**
 * <p>
 * 原子任务表（最小任务单元） Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface BaseTaskMapper extends BaseMapper<BaseTask> {

    /**
     * 相关 EXISTS 跨 base_task / item_task_node / project / item，LambdaQueryWrapper 表达不清，
     * 也不能先载入全部流程节点再按类型过滤。
     */
    List<PlazaTaskTypeOptionResultBO> selectPlazaTaskTypes(@Param("orgId") Long orgId);

}
