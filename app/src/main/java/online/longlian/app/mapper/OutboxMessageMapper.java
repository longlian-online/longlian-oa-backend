package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.OutboxMessage;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 通用 outbox Mapper 接口
 * </p>
 *
 * @author longlian
 * @since
 */
@Mapper
public interface OutboxMessageMapper extends BaseMapper<OutboxMessage> {

}
