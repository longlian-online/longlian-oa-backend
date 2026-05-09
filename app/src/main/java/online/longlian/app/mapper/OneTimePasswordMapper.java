package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.OneTimePassword;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 一次性密码（otp）表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 2026-04-20
 */
@Mapper
public interface OneTimePasswordMapper extends BaseMapper<OneTimePassword> {

}
