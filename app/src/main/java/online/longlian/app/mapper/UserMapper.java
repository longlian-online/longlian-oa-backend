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

    @Update("""
            UPDATE `user`
            SET config = JSON_EXTRACT(#{config}, '$')
            WHERE id = #{userId} AND deleted_at IS NULL
            """)
    int replaceConfig(@Param("userId") Long userId, @Param("config") String config);

    // JSON_MERGE_PATCH 在数据库内合并本次字段；null 删除对应字段，未出现的字段保持原值。
    @Update("""
            UPDATE `user`
            SET config = JSON_MERGE_PATCH(config, JSON_EXTRACT(#{config}, '$'))
            WHERE id = #{userId} AND deleted_at IS NULL
            """)
    int patchConfig(@Param("userId") Long userId, @Param("config") String config);
}
