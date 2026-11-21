package online.longlian.app.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import online.longlian.app.pojo.entity.EmailVerifyOtp;
import online.longlian.common.enumeration.EmailVerifyBusinessType;
import online.longlian.common.enumeration.OTPStatus;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

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

    /**
     * 重发时把同邮箱同业务的有效期窗口内旧验证码一次置为立即过期。
     * 过滤列在扩展表、失效列在验证码表，LambdaUpdateWrapper 无法表达跨表更新。
     */
    @Update("""
            UPDATE one_time_password otp
            INNER JOIN email_verify_otp evo ON evo.otp_id = otp.id
            SET otp.expired_at = #{now}, otp.updated_at = #{now}
            WHERE evo.receiver = #{receiver}
              AND evo.business_type = #{businessType}
              AND evo.created_at > #{windowStart}
              AND otp.status = #{pendingStatus}
            """)
    int expirePreviousCodes(@Param("receiver") String receiver,
                            @Param("businessType") EmailVerifyBusinessType businessType,
                            @Param("windowStart") LocalDateTime windowStart,
                            @Param("now") LocalDateTime now,
                            @Param("pendingStatus") OTPStatus pendingStatus);
}
