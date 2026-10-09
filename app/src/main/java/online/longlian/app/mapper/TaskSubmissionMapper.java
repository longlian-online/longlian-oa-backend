package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.TaskSubmission;
import online.longlian.app.pojo.vo.app.WorkshopProjectInfoVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 任务提交记录表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {

    @Select("""
            <script>
            SELECT latest.project_id AS id,
                   u.username AS last_submitter_username,
                   latest.created_at AS last_submitter_at
            FROM (
                SELECT project_id, submitter_id, created_at,
                       ROW_NUMBER() OVER (PARTITION BY project_id ORDER BY created_at DESC, id DESC) AS row_num
                FROM task_submission
                WHERE deleted_at IS NULL AND project_id IN
                <foreach collection="projectIds" item="projectId" open="(" separator="," close=")">
                    #{projectId}
                </foreach>
            ) latest
            LEFT JOIN `user` u ON u.id = latest.submitter_id AND u.deleted_at IS NULL
            WHERE latest.row_num = 1
            </script>
            """)
    List<WorkshopProjectInfoVO> selectLatestForProjects(@Param("projectIds") List<Long> projectIds);

}
