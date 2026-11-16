package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import online.longlian.common.enumeration.Status;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageStatsResultBO {
    private Long id;

    private String name;

    private Status status;

    private long occupiedBytes;

    private long reclaimedBytes;

    private StorageCategoryStatsResultBO cover;

    private StorageCategoryStatsResultBO thumbnail;

    private StorageCategoryStatsResultBO taskSubmit;
}
