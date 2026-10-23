package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.bo.common.ResourceUsageSummaryBO;
import online.longlian.app.pojo.entity.Resource;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
