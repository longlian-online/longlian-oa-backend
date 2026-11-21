package online.longlian.app.common.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 十六进制摘要（小写）。MessageDigest 非线程安全，方法内新建实例。
 * OpenAPI 密钥散列与幂等请求体哈希共用。
 */
public final class Sha256Util {

    private Sha256Util() {
    }

    public static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(Character.forDigit((b >> 4) & 0xF, 16));
                builder.append(Character.forDigit(b & 0xF, 16));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException e) { // skipcq: TCV-001
            throw new IllegalStateException("SHA-256 不可用", e); // skipcq: TCV-001
        }
    }

    public static String sha256Hex(String input) {
        return sha256Hex(input.getBytes(StandardCharsets.UTF_8));
    }
}
