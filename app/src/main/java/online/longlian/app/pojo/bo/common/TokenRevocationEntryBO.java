package online.longlian.app.pojo.bo.common;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单条凭证吊销记录，使用明确字段避免在业务代码中传递无语义的键值对。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenRevocationEntryBO {
    private String key;
    private Long expiredAtMillis;
}
