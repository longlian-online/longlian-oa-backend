package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.bo.app.PlazaItemStageBO;
import online.longlian.app.pojo.bo.app.PlazaProjectSummaryBO;
import online.longlian.app.pojo.bo.app.PlazaTaskRowBO;
import online.longlian.app.pojo.bo.app.TaskPlazaSearchParamsBO;
import online.longlian.app.pojo.entity.TaskInstance;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * <p>
 * 任务实例表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface TaskInstanceMapper extends BaseMapper<TaskInstance> {

    /** 任务广场：分页返回存在缺人任务的企划聚合行 */
    List<PlazaProjectSummaryBO> selectPlazaProjectSummaries(@Param("params") TaskPlazaSearchParamsBO params,
                                                            @Param("offset") int offset,
                                                            @Param("size") int size);

    /** 任务广场：符合筛选条件的缺人企划总数 */
    long selectPlazaProjectCount(@Param("params") TaskPlazaSearchParamsBO params);

    /** 任务广场：指定企划当前阶段组内的任务实例行（含已占用槽位） */
    List<PlazaTaskRowBO> selectPlazaTaskRows(@Param("params") TaskPlazaSearchParamsBO params,
                                             @Param("projectIds") List<Long> projectIds);

    /** 任务广场：指定条目内全部阶段的 sort 值（推导阶段位次与总数） */
    List<PlazaItemStageBO> selectPlazaItemStages(@Param("itemIds") List<Long> itemIds);
}
