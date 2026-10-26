package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.pojo.bo.app.ProjectSearchParamsBO;
import online.longlian.app.pojo.bo.app.ProjectSearchResultBO;
import online.longlian.app.pojo.entity.Project;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * <p>
 * 企划表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface ProjectMapper extends BaseMapper<Project> {
    /**
     * 标题、别名和 JSON 字段的权重搜索无法用 LambdaQueryWrapper 表达。
     */
    Page<ProjectSearchResultBO> selectSearchPage(Page<ProjectSearchResultBO> page,
                                               @Param("params") ProjectSearchParamsBO params);
}
