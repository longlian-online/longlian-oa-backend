package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.BaseTask;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;
import online.longlian.app.pojo.bo.orgadmin.PlazaTaskTypeOptionResultBO;

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

    /** Cross-table EXISTS avoids loading all workflow nodes or per-type queries. */
    List<PlazaTaskTypeOptionResultBO> selectPlazaTaskTypes(@Param("orgId") Long orgId);

}
