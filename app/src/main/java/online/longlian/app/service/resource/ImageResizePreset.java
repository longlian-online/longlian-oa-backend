package online.longlian.app.service.resource;

import online.longlian.app.pojo.bo.common.ResourceReadUrlGetResultBO;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * EdgeOne 即时图片缩放预设。
 * 统一全站头像、缩略图与列表封面的缩放参数，保证相同资源在不同接口下生成一致的 URL 以复用浏览器缓存。
 */
public enum ImageResizePreset {

    /** 全站小头像（用户头像与组织头像统一规格）。 */
    AVATAR("eo-img.resize=w/64/h/64"),

    /** 企划缩略图与通知缩略图。 */
    THUMBNAIL("eo-img.resize=w/144/h/144"),

    /** 企划列表封面。 */
    COVER("eo-img.resize=w/400/h/600");

    private static final String RESIZE_PARAM_KEY = "eo-img.resize=";

    private final String param;

    ImageResizePreset(String param) {
        this.param = param;
    }

    public String getParam() {
        return param;
    }

    public String apply(String url) {
        if (url == null || url.isBlank() || url.contains(RESIZE_PARAM_KEY)) {
            return url;
        }
        return url + (url.contains("?") ? "&" : "?") + param;
    }

    public Map<Long, String> apply(Map<Long, ResourceReadUrlGetResultBO> readUrls) {
        if (readUrls == null || readUrls.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, String> result = new HashMap<>(readUrls.size());
        readUrls.forEach((id, bo) -> {
            if (bo != null && bo.getUrl() != null) {
                result.put(id, apply(bo.getUrl()));
            }
        });
        return result;
    }
}
