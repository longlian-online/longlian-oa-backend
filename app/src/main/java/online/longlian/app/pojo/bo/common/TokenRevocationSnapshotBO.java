package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 某个凭证身份的吊销快照，Redis 只保存这个对象的序列化结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationSnapshotBO {
    @Builder.Default
    private List<TokenRevocationEntryBO> entries = new ArrayList<>();

    public void merge(String key, long expiredAtMillis) {
        for (TokenRevocationEntryBO entry : entries) {
            if (entry.getKey().equals(key)) {
                entry.setExpiredAtMillis(Math.max(entry.getExpiredAtMillis(), expiredAtMillis));
                return;
            }
        }
        entries.add(TokenRevocationEntryBO.builder().key(key).expiredAtMillis(expiredAtMillis).build());
    }

    public boolean isValid() {
        return entries != null && entries.stream().allMatch(this::isValidEntry);
    }

    private boolean isValidEntry(TokenRevocationEntryBO entry) {
        if (entry == null || entry.getKey() == null || entry.getExpiredAtMillis() == null) {
            return false;
        }
        if (entry.getKey().startsWith("sha256:")) {
            return true;
        }
        if (!entry.getKey().startsWith("before:")) {
            return false;
        }
        try {
            Long.parseLong(entry.getKey().substring("before:".length()));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean containsActiveRevocation(String digest, long issuedAtMillis, long nowMillis) {
        return entries.stream().anyMatch(entry -> entry.getExpiredAtMillis() > nowMillis
                && (entry.getKey().equals(digest)
                || entry.getKey().startsWith("before:")
                && issuedAtMillis <= Long.parseLong(entry.getKey().substring("before:".length()))));
    }
}
