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
    /**
     * 根据用户ID查询关联的角色ID列表
     */
    @Select("""
        SELECT DISTINCT ur.role_id
        FROM user_role ur
        JOIN role r ON ur.role_id = r.id AND r.status = 1
        WHERE ur.user_id = #{userId} AND ur.delete_at IS NULL
    """)
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据角色ID列表查询权限编码
     */
    @Select({
            "<script>",
            "SELECT DISTINCT p.perm_code",
            "FROM role_permission rp",
            "JOIN permission p ON rp.permission_id = p.id ",
            "    AND p.status = 1 ",
            "    AND p.delete_at IS NULL",
            "WHERE rp.role_id IN ",
            "    <foreach collection='roleIds' item='roleId' open='(' separator=',' close=')'>",
            "        #{roleId}",
            "    </foreach>",
            "AND rp.delete_at IS NULL",
            "</script>"
    })
    List<String> selectPermissionCodesByRoleIds(@Param("roleIds") List<Long> roleIds);
    /**
     * 查询用户关联的角色编码
     */
    @Select("""
        SELECT DISTINCT r.role_code
        FROM user_role ur
        JOIN role r ON ur.role_id = r.id AND r.status = 1
        WHERE ur.user_id = #{userId} AND ur.delete_at IS NULL
    """)
    List<String> selectRoleByUserId(@Param("userId") Long userId);
}