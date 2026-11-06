package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.TaskDraft;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 任务暂存成果快照 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-11-06
 */
@Mapper
public interface TaskDraftMapper extends BaseMapper<TaskDraft> {

}
