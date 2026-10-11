package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.Role;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 角色表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Deprecated(since = "2026-09", forRemoval = false)
@Mapper
public interface RoleMapper extends BaseMapper<Role> {

}
