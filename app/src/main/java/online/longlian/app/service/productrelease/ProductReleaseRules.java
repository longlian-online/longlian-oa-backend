package online.longlian.app.service.productrelease;

import online.longlian.app.common.exception.AppException;
import online.longlian.app.common.result.ResultCode;

import java.util.ArrayList;
import java.util.List;
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
            List<String> added,
            List<String> changed,
            List<String> fixed,
            boolean requirePublishable
    ) {
        ProductReleaseContent content = new ProductReleaseContent(
                normalizeVersion(version),
                normalizeVersion(frontendVersion),
                normalizeVersion(backendVersion),
                normalizeText(title, 40, "标题"),
                normalizeText(summary, 120, "摘要"),
                normalizeItems(added, "新增"),
                normalizeItems(changed, "调整"),
                normalizeItems(fixed, "修复")
        );
        if (requirePublishable) {
            requirePublishable(content);
        }
        return content;
    }

    public static void requirePublishable(ProductReleaseContent content) {
        if (content.title().isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "标题不能为空");
        }
        if (content.summary().isEmpty()) {
            throw new AppException(ResultCode.PARAM_ERROR, "摘要不能为空");
        }
        if (!content.hasChange()) {
            throw new AppException(ResultCode.PARAM_ERROR, "至少写一条变更");
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

    private static List<String> normalizeItems(List<String> items, String label) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        if (items.size() > 20) {
            throw new AppException(ResultCode.PARAM_ERROR, label + "最多 20 条");
        }
        List<String> normalized = new ArrayList<>(items.size());
        for (String item : items) {
            String value = item == null ? "" : item.trim();
            if (value.isEmpty()) {
                throw new AppException(ResultCode.PARAM_ERROR, label + "不能有空白条目");
            }
            if (value.length() > 80) {
                throw new AppException(ResultCode.PARAM_ERROR, label + "每条最多 80 字");
            }
            normalized.add(value);
        }
        return List.copyOf(normalized);
    }
}
