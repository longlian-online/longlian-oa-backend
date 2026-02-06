package online.longlian.app.service;

import cn.hutool.core.util.RandomUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import online.longlian.app.common.constants.CommonConstants;
import online.longlian.app.common.constants.RedisConstants;
import online.longlian.app.service.notify.NotificationManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VerifyCodeService {

    private final StringRedisTemplate stringRedisTemplate;
    private final NotificationManager notificationManager;
    // 发件人邮箱（从配置文件获取）
    @Value("${spring.mail.username}")
    private String fromEmail;

    /**
     * 生成6位数字验证码
     */
    public String generateCode() {
        return RandomUtil.randomNumbers(CommonConstants.CODE_LENGTH);
    }

    public boolean sendCode(String receiver) {
        // 1. 1分钟内是否已发送
        if (stringRedisTemplate.hasKey(RedisConstants.LIMIT_KEY + receiver)) {
            log.warn("{}发送验证码过于频繁，需等待{}秒", receiver, CommonConstants.CODE_LIMIT);
            return false;
        }
        // 2. 生成验证码
        String code = generateCode();
        // 3. 发送简单邮件
        try {
            notificationManager.send(receiver, code);
            log.info("{}验证码发送成功：{}", receiver, code);
        } catch (Exception e) {
            log.error("{}验证码发送失败", receiver);
            return false;
        }
        // 4. Redis存储验证码（5分钟有效期）
        stringRedisTemplate.opsForValue().set(RedisConstants.CODE_KEY + receiver, code, CommonConstants.CODE_EXPIRE, TimeUnit.SECONDS);
        stringRedisTemplate.opsForValue().set(RedisConstants.LIMIT_KEY + receiver, "1", CommonConstants.CODE_LIMIT, TimeUnit.SECONDS);
        return true;
    }

    public boolean validateCode(String receiver, String code) {
        // 1. 检查Redis中是否存在验证码
        String redisCode = stringRedisTemplate.opsForValue().get(RedisConstants.CODE_KEY + receiver);
        if (redisCode == null) {
            log.warn("{}验证码已过期或未发送", receiver);
            return false;
        }
        // 2. 校验验证码是否一致
        boolean isValid = redisCode.equals(code);
        // 3. 验证成功后删除验证码
        if (isValid) {
            stringRedisTemplate.delete(RedisConstants.CODE_KEY + receiver);
        }
        return isValid;
    }
}