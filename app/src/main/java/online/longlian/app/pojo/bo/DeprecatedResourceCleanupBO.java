package online.longlian.app.pojo.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.StorageType;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeprecatedResourceCleanupBO {
    private Long id;
    private StorageType storageType;
    private String storageKey;
    private Integer cleanupAttempts;
}
