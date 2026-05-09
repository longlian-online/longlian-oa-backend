package online.longlian.app.common.util;

import lombok.NoArgsConstructor;
import java.util.Random;

/**
 * 随机码工具类
 */
@NoArgsConstructor
public final class RandomCodeUtil {

    private static final Random RANDOM = new Random();
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