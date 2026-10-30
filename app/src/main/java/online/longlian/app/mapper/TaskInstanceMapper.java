package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.bo.app.ClaimedTaskLoadRowBO;
import online.longlian.app.pojo.bo.app.ItemStageNodeStatusBO;
import online.longlian.app.pojo.bo.app.MyTaskRowBO;
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

    /** 任务广场：指定企划匹配阶段范围内的实例行（含已占用槽位） */
    List<PlazaTaskRowBO> selectPlazaTaskRows(@Param("params") TaskPlazaSearchParamsBO params,
                                             @Param("projectIds") List<Long> projectIds);

    /** 任务广场：指定条目内全部阶段的 sort 值（推导阶段位次与总数） */
    List<PlazaItemStageBO> selectPlazaItemStages(@Param("itemIds") List<Long> itemIds);

    /** 我的待办：当前用户在当前组织内已接取（CLAIMED）的任务，按接取时间排序 */
    List<MyTaskRowBO> selectMyClaimedTasks(@Param("userId") Long userId,
                                           @Param("orgId") Long orgId,
                                           @Param("desc") boolean desc);

    /**
     * 组织负载：指定成员在当前组织内已接取（CLAIMED）的任务，按接取时间、实例 ID 升序。
     * 空的接取人列表不要调用，IN 为空会产生非法 SQL。
     */
    List<ClaimedTaskLoadRowBO> selectClaimedTasksByAssignees(
            @Param("orgId") Long orgId,
            @Param("assigneeIds") List<Long> assigneeIds);

    /** 指定项目内全部任务节点及其完成情况（前置判定用） */
    List<ItemStageNodeStatusBO> selectItemStageNodeStatuses(@Param("itemIds") List<Long> itemIds);
}
