package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 * 邮箱验证码扩展表 Mapper 接口
 * </p>
 *
 * @author longlian
 * @since 
 */
@Mapper
public interface EmailVerifyOtpMapper extends BaseMapper<EmailVerifyOtp> {

}
