package online.longlian.app.pojo.bo.common;

/** 本地文件读取结果：内容与需要回写到响应头的 Content-Disposition（null 表示不设置）。 */
public record LocalFileReadResult(
        org.springframework.core.io.Resource content,
        String contentDisposition) {
}
