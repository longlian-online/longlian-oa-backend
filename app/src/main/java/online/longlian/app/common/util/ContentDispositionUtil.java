package online.longlian.app.common.util;

/**
 * Content-Disposition 头值工具类。
 * <p>
 * 生成值随对象存储上传写入对象元数据，读取时由存储/CDN 透传给浏览器，
 * 使下载文件还原原始文件名。
 */
public final class ContentDispositionUtil {

    private static final String ATTACHMENT_PREFIX = "attachment; filename=\"";
    private static final String EXT_VALUE_PREFIX = "\"; filename*=UTF-8''";
    private static final String FALLBACK_FILE_NAME = "file";
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();

    private ContentDispositionUtil() {
    }

    /**
     * 按 RFC 6266 生成 attachment 头值。
     * <p>
     * filename* 按 RFC 5987 百分号编码携带原始 UTF-8 文件名；
     * filename 提供仅含可见 ASCII 的兜底名，兼容不支持 filename* 的用户代理。
     * 输入中的控制字符（含 CRLF）一律移除，防止响应头注入。
     */
    public static String attachment(String fileName) {
        String sanitized = removeControlChars(fileName);
        return ATTACHMENT_PREFIX + fallback(sanitized) + EXT_VALUE_PREFIX + percentEncode(sanitized);
    }

    private static String removeControlChars(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder(fileName.length());
        for (int index = 0; index < fileName.length(); index++) {
            char current = fileName.charAt(index);
            if (current > 0x1F && current != 0x7F) {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    /** 生成 quoted-string 兜底名：仅保留可见 ASCII，反斜杠与双引号转义，无可用字符时回退固定名。 */
    private static String fallback(String sanitized) {
        StringBuilder builder = new StringBuilder(sanitized.length());
        for (int index = 0; index < sanitized.length(); index++) {
            char current = sanitized.charAt(index);
            if (current >= 0x20 && current < 0x7F) {
                if (current == '\\' || current == '"') {
                    builder.append('\\');
                }
                builder.append(current);
            } else {
                builder.append('_');
            }
        }
        String asciiOnly = builder.toString();
        return asciiOnly.isEmpty() ? FALLBACK_FILE_NAME : asciiOnly;
    }

    /** RFC 5987 attr-char 之外的字符一律按 UTF-8 百分号编码，空格编码为 %20。 */
    private static String percentEncode(String sanitized) {
        if (sanitized.isEmpty()) {
            return FALLBACK_FILE_NAME;
        }
        StringBuilder builder = new StringBuilder(sanitized.length());
        byte[] utf8 = sanitized.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        for (byte current : utf8) {
            if (current >= 0 && isUnreserved((char) current)) {
                builder.append((char) current);
            } else {
                builder.append('%').append(HEX[(current & 0xF0) >> 4]).append(HEX[current & 0x0F]);
            }
        }
        return builder.toString();
    }

    private static boolean isUnreserved(char current) {
        return (current >= 'A' && current <= 'Z')
                || (current >= 'a' && current <= 'z')
                || (current >= '0' && current <= '9')
                || current == '-' || current == '.' || current == '_' || current == '~';
    }
}
