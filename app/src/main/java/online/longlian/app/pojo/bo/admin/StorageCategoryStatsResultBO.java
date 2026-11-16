package online.longlian.app.pojo.bo.admin;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StorageCategoryStatsResultBO {
    private StorageStateStatsResultBO unbound;

    private StorageStateStatsResultBO active;

    private StorageStateStatsResultBO retention;

    private StorageStateStatsResultBO recycled;
}
