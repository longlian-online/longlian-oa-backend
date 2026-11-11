package online.longlian.app.pojo.bo.app;

import java.time.LocalDateTime;

public record UserOrgApiKeyResultBO(Long id, Long orgId, String state, String prefix,
                                    LocalDateTime generatedAt, LocalDateTime revokedAt,
                                    String suspendedReason, boolean canGenerate, boolean canRevoke) {
}
