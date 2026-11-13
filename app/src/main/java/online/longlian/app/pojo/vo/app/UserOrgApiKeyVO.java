package online.longlian.app.pojo.vo.app;

import java.time.LocalDateTime;

public record UserOrgApiKeyVO(String id, String orgId, String state, String prefix, LocalDateTime generatedAt,
                              LocalDateTime revokedAt, String suspendedReason, boolean canGenerate, boolean canRevoke) {
}
