package online.longlian.app.common.util;

import java.security.SecureRandom;
import lombok.NoArgsConstructor;

/**
 * 随机码工具类
 */
@NoArgsConstructor
public final class RandomCodeUtil {

    // 一次性密码可被预测即失效，必须使用密码学安全的随机源
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";

    /**
     * 生成指定长度的随机码（包含大写字母和数字）
     */
    public static String generateCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive: " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }
}