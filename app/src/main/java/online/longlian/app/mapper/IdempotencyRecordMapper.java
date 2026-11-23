package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.IdempotencyRecord;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 写请求幂等记录表，按来源区分调用方 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface IdempotencyRecordMapper extends BaseMapper<IdempotencyRecord> {

}
