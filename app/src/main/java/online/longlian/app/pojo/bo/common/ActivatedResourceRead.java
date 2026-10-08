package online.longlian.app.pojo.bo.common;

/** Signed read facts for an activated resource. Callers format these for their own response. */
public record ActivatedResourceRead(
        Long id,
        String fileName,
        Long fileSize,
        String fileMime,
        String readUrl,
        long expiresAt) {
}
