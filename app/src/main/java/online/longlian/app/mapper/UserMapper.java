package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

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

    // JSON_SET 在数据库内只更新一个配置键，避免覆盖其他配置或并发更新。
    @Update("""
            UPDATE `user`
            SET config = JSON_SET(config, '$.emailNotificationsEnabled', JSON_EXTRACT(#{enabled}, '$'))
            WHERE id = #{userId} AND deleted_at IS NULL
            """)
    int updateEmailNotifications(@Param("userId") Long userId, @Param("enabled") String enabled);
}
