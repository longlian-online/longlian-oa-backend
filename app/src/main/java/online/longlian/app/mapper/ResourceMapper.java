package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.bo.common.ResourceUsageSummaryBO;
import online.longlian.app.pojo.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 通用文件存储表 Mapper 接口
 * </p>
 *
 * @author longlian
 */
@Mapper
public interface ResourceMapper extends BaseMapper<Resource> {

    /**
     * 按组织、业务类型和存储状态汇总。Wrapper 无法表达这组分状态条件聚合。
     */
    @Select("""
            <script>
            SELECT org_id AS orgId,
                   biz_type AS bizType,
                   CASE
                     WHEN storage_cleaned_at IS NOT NULL THEN 'recycled'
                     WHEN process_status = 1 THEN 'active'
                     WHEN process_status = 2 AND biz_id != 0 THEN 'retention'
                     ELSE 'unbound'
                   END AS state,
                   COUNT(*) AS fileCount,
                   CAST(COALESCE(SUM(file_size), 0) AS SIGNED) AS bytes
            FROM resource
            WHERE deleted_at IS NULL
              AND biz_type IN ('cover', 'thumbnail', 'task_submit')
              AND org_id IN
              <foreach collection="orgIds" item="orgId" open="(" separator="," close=")">#{orgId}</foreach>
            GROUP BY org_id, biz_type, state
            </script>
            """)
    List<ResourceUsageSummaryBO> summarizeUsage(@Param("orgIds") List<Long> orgIds);

    /**
     * 业务表上的当前文件编号无法用单表 Wrapper 表达。
     * 提交内容和任务暂存的 fileId 都在 JSON 里，LIKE 带上结束引号，避免 12 命中 123。
     * 判断和更新在同一条语句里完成，避免扫描期间刚保存的引用被标成弃置。
     */
    @Update("""
            UPDATE resource
            SET process_status = #{deprecatedStatus},
                deprecated_at = #{executeTime},
                updated_at = #{executeTime}
            WHERE id = #{resourceId}
              AND deleted_at IS NULL
              AND process_status = #{activatedStatus}
              AND storage_cleaned_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM project
                  WHERE deleted_at IS NULL
                    AND (cover_file_id = #{resourceId} OR thumbnail_file_id = #{resourceId})
              )
              AND NOT EXISTS (
                  SELECT 1 FROM `user`
                  WHERE deleted_at IS NULL AND avatar_file_id = #{resourceId}
              )
              AND NOT EXISTS (
                  SELECT 1 FROM organization
                  WHERE deleted_at IS NULL AND avatar_file_id = #{resourceId}
              )
              AND NOT EXISTS (
                  SELECT 1 FROM project_attachment
                  WHERE deleted_at IS NULL AND file_id = #{resourceId}
              )
              AND NOT EXISTS (
                  SELECT 1 FROM task_submission submission
                  INNER JOIN task_instance instance
                      ON instance.id = submission.task_instance_id AND instance.deleted_at IS NULL
                  WHERE submission.deleted_at IS NULL
                    AND submission.metadata LIKE CONCAT('%"fileId":"', #{resourceId}, '"%')
              )
              AND NOT EXISTS (
                  SELECT 1 FROM task_instance draft_instance
                  WHERE draft_instance.deleted_at IS NULL
                    AND draft_instance.draft_metadata LIKE CONCAT('%"fileId":"', #{resourceId}, '"%')
              )
            """)
    int deprecateIfUnreferenced(@Param("resourceId") Long resourceId,
                                @Param("executeTime") LocalDateTime executeTime,
                                @Param("activatedStatus") int activatedStatus,
                                @Param("deprecatedStatus") int deprecatedStatus);

    /**
     * 组织文件页主动弃置任务提交文件。提交内容和暂存仍可能引用它，那些页面会显示为不可用。
     * 封面、头像和企划附件仍引用时不弃置，避免误伤其他资料。
     */
    @Update("""
            UPDATE resource
            SET process_status = #{deprecatedStatus},
                deprecated_at = #{executeTime},
                updated_at = #{executeTime}
            WHERE id = #{resourceId}
              AND org_id = #{orgId}
              AND biz_type = 'task_submit'
              AND biz_id = #{taskInstanceId}
              AND deleted_at IS NULL
              AND process_status = #{activatedStatus}
              AND storage_cleaned_at IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM project
                  WHERE deleted_at IS NULL
                    AND (cover_file_id = #{resourceId} OR thumbnail_file_id = #{resourceId})
              )
              AND NOT EXISTS (
                  SELECT 1 FROM `user`
                  WHERE deleted_at IS NULL AND avatar_file_id = #{resourceId}
              )
              AND NOT EXISTS (
                  SELECT 1 FROM organization
                  WHERE deleted_at IS NULL AND avatar_file_id = #{resourceId}
              )
              AND NOT EXISTS (
                  SELECT 1 FROM project_attachment
                  WHERE deleted_at IS NULL AND file_id = #{resourceId}
              )
            """)
    int deprecateTaskSubmitFile(@Param("resourceId") Long resourceId,
                                @Param("taskInstanceId") Long taskInstanceId,
                                @Param("orgId") Long orgId,
                                @Param("executeTime") LocalDateTime executeTime,
                                @Param("activatedStatus") int activatedStatus,
                                @Param("deprecatedStatus") int deprecatedStatus);
}
