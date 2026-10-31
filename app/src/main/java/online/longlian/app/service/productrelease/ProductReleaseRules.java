package online.longlian.app.service.productrelease;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

import java.util.regex.Pattern;

public final class ProductReleaseRules {

    private static final Pattern VERSION = Pattern.compile("^(0|[1-9]\\d{0,6})\\.(0|[1-9]\\d{0,6})\\.(0|[1-9]\\d{0,6})$");

    private ProductReleaseRules() {
    }

    public static ProductReleaseContent normalize(
            String version,
            String frontendVersion,
            String backendVersion,
            String title,
            String summary,
            String content,
            boolean requirePublishable
    ) {
        ProductReleaseContent normalized = new ProductReleaseContent(
                normalizeVersion(version),
                normalizeVersion(frontendVersion),
                normalizeVersion(backendVersion),
                normalizeText(title, 40, "标题"),
                normalizeText(summary, 120, "摘要"),
                normalizeText(content, 4000, "更新说明")
        );
        if (requirePublishable) {
            requirePublishable(normalized);
        }
        return normalized;
    }

    public static void requirePublishable(ProductReleaseContent content) {
        if (content.title().isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "标题不能为空");
        }
        if (content.summary().isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "摘要不能为空");
        }
        if (content.content().isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "更新说明不能为空");
        }
    }

    public static String normalizeVersion(String raw) {
        if (raw == null) {
            throw new AppException(ResultCode.PARAM_ERROR, "版本格式不正确");
        }
        String value = raw.trim();
        if (value.length() >= 2 && (value.charAt(0) == 'v' || value.charAt(0) == 'V')) {
            value = value.substring(1);
        }
        if (!VERSION.matcher(value).matches()) {
            throw new AppException(ResultCode.PARAM_ERROR, "版本格式不正确");
        }
        return value;
    }

    private static String normalizeText(String raw, int max, String label) {
        String value = raw == null ? "" : raw.trim();
        if (value.length() > max) {
            throw new AppException(ResultCode.PARAM_ERROR, label + "最多 " + max + " 字");
        }
        return value;
    }
}
