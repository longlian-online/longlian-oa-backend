package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.TaskSubmission;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 任务提交记录表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-03-17
 */
@Mapper
public interface TaskSubmissionMapper extends BaseMapper<TaskSubmission> {

}
