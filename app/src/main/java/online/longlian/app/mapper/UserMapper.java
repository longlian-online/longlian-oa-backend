package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

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

    @Delete("DELETE FROM `user` WHERE id = #{userId}")
    int deletePhysicallyById(@Param("userId") Long userId);
}
