package online.longlian.app.pojo.bo.common;

/** 已激活资源的签名读取结果。调用方按自己的响应格式组装。 */
public record ActivatedResourceRead(
        Long id,
        String fileName,
        Long fileSize,
        String fileMime,
        String readUrl,
        long expiresAt) {
}
