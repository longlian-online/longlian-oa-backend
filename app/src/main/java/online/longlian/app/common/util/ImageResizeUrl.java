package online.longlian.app.common.util;

/**
 * 给已有图片地址追加 EdgeOne 缩放参数。空地址由调用方决定是否跳过。
 */
public final class ImageResizeUrl {

    private ImageResizeUrl() {
    }

    public static String append(String url, String param) {
        return url + (url.contains("?") ? "&" : "?") + param;
    }
}
