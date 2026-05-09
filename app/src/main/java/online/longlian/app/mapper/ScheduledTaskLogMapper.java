package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.ScheduledTaskLog;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 定时任务执行日志表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface ScheduledTaskLogMapper extends BaseMapper<ScheduledTaskLog> {

}
