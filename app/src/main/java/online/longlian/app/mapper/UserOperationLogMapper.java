package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.UserOperationLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 用户操作记录表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface UserOperationLogMapper extends BaseMapper<UserOperationLog> {

}
