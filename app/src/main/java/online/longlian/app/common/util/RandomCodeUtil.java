package online.longlian.app.common.util;

import lombok.NoArgsConstructor;
import java.security.SecureRandom;

/**
 * 随机码工具类
 */
@NoArgsConstructor
public final class RandomCodeUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    /**
     * 生成指定长度的数字随机码
     */
    public static String generateCode(int length) {
        if (length <= 0) {
            throw new IllegalArgumentException("Length must be positive: " + length);
        }
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}