package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.TokenBlacklist;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * Token黑名单表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface TokenBlacklistMapper extends BaseMapper<TokenBlacklist> {

}
