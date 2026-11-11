package online.longlian.app.pojo.vo.app;

import java.time.LocalDateTime;

public record UserOrgApiKeyGenerateVO(String id, String orgId, String apiKey, String prefix, LocalDateTime generatedAt, String mcpUrl) {
    @Override
    public String toString() {
        return "UserOrgApiKeyGenerateVO[id=" + id + ", orgId=" + orgId + "]";
    }
}
