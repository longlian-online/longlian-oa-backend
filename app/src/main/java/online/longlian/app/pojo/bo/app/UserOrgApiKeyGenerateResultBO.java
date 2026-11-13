package online.longlian.app.pojo.bo.app;

import java.time.LocalDateTime;

public record UserOrgApiKeyGenerateResultBO(Long id, Long orgId, String apiKey, String prefix, LocalDateTime generatedAt) {
    @Override
    public String toString() {
        return "UserOrgApiKeyGenerateResultBO[id=" + id + ", orgId=" + orgId + "]";
    }
}
