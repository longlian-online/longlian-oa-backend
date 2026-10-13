package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import online.longlian.app.pojo.bo.app.WorkshopListParamsBO;
import online.longlian.app.pojo.entity.Project;
import org.apache.ibatis.annotations.Param;
import online.longlian.app.pojo.entity.ProjectWorkshop;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 企划-工坊关联表（用户添加企划） Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface ProjectWorkshopMapper extends BaseMapper<ProjectWorkshop> {
    Page<Project> selectWorkshopProjects(Page<Project> page,
            @Param("params") WorkshopListParamsBO params, @Param("typeId") Long typeId);

}
