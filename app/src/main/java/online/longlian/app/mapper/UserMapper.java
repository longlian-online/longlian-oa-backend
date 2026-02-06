package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * <p>
 * 系统用户表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-02-04
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {
    @Select("""
        SELECT DISTINCT p.perm_code
            FROM user_role ur
            JOIN role r
                ON ur.role_id = r.id
               AND r.status = 1
            JOIN role_permission rp
                ON rp.role_id = r.id
            JOIN permission p
                ON p.id = rp.permission_id
               AND p.status = 1
            WHERE ur.user_id = #{userId}
              AND ur.delete_at IS NULL
              AND rp.delete_at IS NULL
              AND p.delete_at IS NULL;
    """)
    List<String> selectPermissionByUserId(Long userId);
    @Select("SELECT DISTINCT r.role_code " +
            "FROM user_role ur " +
            "JOIN role r ON ur.role_id = r.id AND r.status = 1 " +
            "WHERE ur.user_id = #{userId} AND ur.delete_at IS NULL")
    List<String> selectRoleByUserId(@Param("userId") Long userId);
}
